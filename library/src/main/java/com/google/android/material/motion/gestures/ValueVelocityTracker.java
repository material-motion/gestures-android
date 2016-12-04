/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.gestures;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A velocity tracker for any arbitrary value. Uses a {@link VelocityTracker} under the hood which
 * is fed specially crafted {@link MotionEvent}s.
 */
class ValueVelocityTracker {

  /**
   * A type of value that is accumulated as a additive sum.
   */
  public static final int ADDITIVE = 0;

  /**
   * A type of value that is accumulated as a multiplicative product.
   */
  public static final int MULTIPLICATIVE = 1;

  /**
   * A type that describes how a value is accumulated.
   */
  @IntDef({ADDITIVE, MULTIPLICATIVE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface AccumulationType {

  }

  private static final int PIXELS_PER_SECOND = 1000;
  private static final float DONT_CARE = 0f;

  private final float maximumFlingVelocity;
  @AccumulationType
  private final int type;

  @Nullable
  private VelocityTracker velocityTracker;
  private float adjust;
  private float currentVelocity;

  public ValueVelocityTracker(Context context, @AccumulationType int type) {
    this.maximumFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    this.type = type;
  }

  /**
   * Returns the velocity calculated in the most recent {@link #onGestureEnd(MotionEvent,
   * float)}.
   */
  public float getCurrentVelocity() {
    return currentVelocity;
  }

  /**
   * Processes the start of a gesture.
   * <p>
   * Must be balanced with a call to {@link #onGestureEnd(MotionEvent, float)} to end the
   * gesture.
   */
  public void onGestureStart(MotionEvent event, float value) {
    velocityTracker = VelocityTracker.obtain();
    if (type == ADDITIVE) {
      adjust = 0f;
    } else {
      adjust = 1f;
    }
    currentVelocity = 0f;

    addValueMovement(event, value);
  }

  /**
   * Processes the adjustment of a gesture. Call this if you do not want the value to jump
   * discontinuously on additional fingers entering and exiting the gesture.
   * <p>
   * May be called multiple times during a gesture.
   */
  public void onGestureAdjust(float adjust) {
    this.adjust = adjust;
  }

  /**
   * Processes the movement of a gesture.
   * <p>
   * May be called multiple times during a gesture.
   */
  public void onGestureMove(MotionEvent event, float value) {
    addValueMovement(event, value);
  }

  /**
   * Processes the end of a gesture.
   * <p>
   * Must be balanced with a previous call to {@link #onGestureStart(MotionEvent, float)}.
   */
  public void onGestureEnd(MotionEvent event, float value) {
    addValueMovement(event, value);

    velocityTracker.computeCurrentVelocity(PIXELS_PER_SECOND, maximumFlingVelocity);
    currentVelocity = velocityTracker.getXVelocity();

    velocityTracker.recycle();
    velocityTracker = null;
  }

  private void addValueMovement(MotionEvent event, float value) {
    int valueMovementAction;

    int action = MotionEventCompat.getActionMasked(event);
    switch (action) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_MOVE:
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        valueMovementAction = action;
        break;
      case MotionEvent.ACTION_POINTER_DOWN:
        valueMovementAction = MotionEvent.ACTION_DOWN;
        break;
      case MotionEvent.ACTION_POINTER_UP:
        valueMovementAction = MotionEvent.ACTION_UP;
        break;
      default:
        throw new IllegalArgumentException("Unexpected action for event: " + event);
    }
    velocityTracker.addMovement(
      MotionEvent.obtain(
        event.getDownTime(),
        event.getEventTime(),
        valueMovementAction,
        apply(value, adjust),
        DONT_CARE,
        event.getMetaState()));
  }

  private float apply(float value, float adjust) {
    if (type == ADDITIVE) {
      return value + adjust;
    } else {
      return value * adjust;
    }
  }
}
