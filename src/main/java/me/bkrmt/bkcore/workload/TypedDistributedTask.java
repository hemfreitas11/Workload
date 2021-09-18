/*
 * MIT License
 *
 * Copyright (c) 2021 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package me.bkrmt.bkcore.workload;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * a class that represents typed distributed tasks.
 *
 * @param <T> type of the work.
 */
public final class TypedDistributedTask<T> implements Runnable {

  /**
   * the action.
   */
  private final Consumer<T> action;

  /**
   * the distribution size.
   */
  private final int distributionSize;

  /**
   * the escape condition.
   */
  private final Predicate<T> escapeCondition;

  /**
   * the world-load matrix.
   */
  private final List<LinkedList<Supplier<T>>> suppliedValueMatrix;

  /**
   * the current position.
   */
  private int currentPosition = 0;

  /**
   * ctor.
   *
   * @param action the action.
   * @param escapeCondition the escape condition.
   * @param distributionSize the distribution size.
   */
  public TypedDistributedTask(final Consumer<T> action, final Predicate<T> escapeCondition,
                              final int distributionSize) {
    this.distributionSize = distributionSize;
    this.action = action;
    this.escapeCondition = escapeCondition;
    this.suppliedValueMatrix = new ArrayList<>(this.distributionSize);
    IntStream.range(0, this.distributionSize)
      .<LinkedList<Supplier<T>>>mapToObj(index -> new LinkedList<>())
      .forEach(this.suppliedValueMatrix::add);
  }

  public static <T> TypedDistributedTaskBuilder<T> builder() {
    return new TypedDistributedTaskBuilder<T>();
  }

  /**
   * adds the given workload into the {@link #suppliedValueMatrix}.
   *
   * @param workload the workload to add.
   */
  public void add(final Supplier<T> workload) {
    LinkedList<Supplier<T>> smallestList = this.suppliedValueMatrix.get(0);
    for (int index = 0; index < this.distributionSize; index++) {
      if (smallestList.size() == 0) {
        break;
      }
      final LinkedList<Supplier<T>> next = this.suppliedValueMatrix.get(index);
      final int size = next.size();
      if (size < smallestList.size()) {
        smallestList = next;
      }
    }
    smallestList.add(workload);
  }

  @Override
  public void run() {
    this.suppliedValueMatrix.get(this.currentPosition).removeIf(this::executeThenCheck);
    this.proceedPosition();
  }

  /**
   * executes the given value supplier then checks {@link #escapeCondition}.
   *
   * @param valueSupplier the value supplier to execute and check.
   *
   * @return {@code true} if {@link #escapeCondition} is null or the test succeeded.
   */
  private boolean executeThenCheck(final Supplier<T> valueSupplier) {
    final T value = valueSupplier.get();
    if (this.action != null) {
      this.action.accept(value);
    }
    return this.escapeCondition == null || this.escapeCondition.test(value);
  }

  /**
   * processes the {@link #currentPosition}.
   */
  private void proceedPosition() {
    if (++this.currentPosition == this.distributionSize) {
      this.currentPosition = 0;
    }
  }

  public static class TypedDistributedTaskBuilder<T> {
    private Consumer<T> action;
    private Predicate<T> escapeCondition;
    private int distributionSize;

    public TypedDistributedTaskBuilder<T> action(Consumer<T> action) {
      this.action = action;
      return this;
    }

    public TypedDistributedTaskBuilder<T> escapeCondition(Predicate<T> escapeCondition) {
      this.escapeCondition = escapeCondition;
      return this;
    }

    public TypedDistributedTaskBuilder<T> distributionSize(int distributionSize) {
      this.distributionSize = distributionSize;
      return this;
    }

    public TypedDistributedTask<T> build() {
      return new TypedDistributedTask<T>(action, escapeCondition, distributionSize);
    }

    public String toString() {
      return "TypedDistributedTask.TypedDistributedTaskBuilder(action=" + this.action + ", escapeCondition=" + this.escapeCondition + ", distributionSize=" + this.distributionSize + ")";
    }
  }
}
