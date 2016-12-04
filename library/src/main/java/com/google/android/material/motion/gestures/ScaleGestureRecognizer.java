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
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import static com.google.android.material.motion.gestures.ValueVelocityTracker.MULTIPLICATIVE;

/**
 * A gesture recognizer that generates scale events.
 */
public class ScaleGestureRecognizer extends GestureRecognizer {

  /**
   * Touch slop for scale. Amount of pixels that the span needs to change.
   */
  public int scaleSlop = UNSET_SLOP;

  private float currentCentroidX;
  private float currentCentroidY;

  private float initialSpan;
  private float currentSpan;

  private ValueVelocityTracker spanVelocityTracker;

  @Override
  public void setElement(@Nullable View element) {
    super.setElement(element);

    if (element == null) {
      return;
    }

    if (scaleSlop == UNSET_SLOP) {
      Context context = element.getContext();
      this.scaleSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    spanVelocityTracker = new ValueVelocityTracker(element.getContext(), MULTIPLICATIVE);
  }

  public boolean onTouchEvent(MotionEvent event) {
    PointF centroid = calculateUntransformedCentroid(event);
    float centroidX = centroid.x;
    float centroidY = centroid.y;
    float span = calculateAverageSpan(event, centroidX, centroidY);

    int action = MotionEventCompat.getActionMasked(event);
    int pointerCount = event.getPointerCount();
    if (action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      initialSpan = span;
      currentSpan = span;

      spanVelocityTracker.onGestureStart(event, span);

      if (scaleSlop == 0) {
        setState(BEGAN);
      }
    }
    if (action == MotionEvent.ACTION_POINTER_DOWN && pointerCount > 2
      || action == MotionEvent.ACTION_POINTER_UP && pointerCount > 2) {
      float adjustX = centroidX - currentCentroidX;
      float adjustY = centroidY - currentCentroidY;

      currentCentroidX += adjustX;
      currentCentroidY += adjustY;

      float adjustSpan = span / currentSpan;

      initialSpan *= adjustSpan;
      currentSpan *= adjustSpan;

      spanVelocityTracker.onGestureAdjust(1 / adjustSpan);
    }
    if (action == MotionEvent.ACTION_MOVE && pointerCount >= 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      if (!isInProgress()) {
        float deltaSpan = span - initialSpan;
        if (Math.abs(deltaSpan) > scaleSlop) {
          float adjustSpan = 1 + Math.signum(deltaSpan) * (scaleSlop / initialSpan);

          initialSpan *= adjustSpan;
          currentSpan *= adjustSpan;

          setState(BEGAN);
        }
      }

      if (isInProgress()) {
        currentSpan = span;

        setState(CHANGED);
      }

      spanVelocityTracker.onGestureMove(event, span);
    }
    if (action == MotionEvent.ACTION_POINTER_UP && pointerCount == 2
      || action == MotionEvent.ACTION_CANCEL && pointerCount >= 2) {
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      initialSpan = 0;
      currentSpan = 0;

      spanVelocityTracker.onGestureEnd(event, span);

      if (isInProgress()) {
        if (action == MotionEvent.ACTION_POINTER_UP) {
          setState(RECOGNIZED);
        } else {
          setState(CANCELLED);
        }
      }
    }

    return true;
  }

  /**
   * Returns the scale of the pinch gesture.
   * <p>
   * This reports the total scale over time since the {@link #BEGAN beginning} of the gesture.
   * This is not a delta value from the last {@link #CHANGED update}.
   */
  public float getScale() {
    return initialSpan > 0 ? currentSpan / initialSpan : 1;
  }

  /**
   * Returns the scalar velocity of the scale gesture.
   * <p>
   * Only read this when the state is {@link #RECOGNIZED} or {@link #CANCELLED}.
   *
   * @return The velocity in pixels per second.
   */
  public float getVelocity() {
    return spanVelocityTracker.getCurrentVelocity();
  }

  @Override
  public float getUntransformedCentroidX() {
    return currentCentroidX;
  }

  @Override
  public float getUntransformedCentroidY() {
    return currentCentroidY;
  }

  /**
   * Calculates the average span of all the active pointers in the given motion event.
   * <p>
   * The average span is twice the average distance of all pointers to the given centroid.
   */
  private float calculateAverageSpan(MotionEvent event, float centroidX, float centroidY) {
    int action = MotionEventCompat.getActionMasked(event);
    int index = MotionEventCompat.getActionIndex(event);

    float sum = 0;
    int num = 0;
    for (int i = 0, count = event.getPointerCount(); i < count; i++) {
      if (action == MotionEvent.ACTION_POINTER_UP && index == i) {
        continue;
      }

      sum += calculateDistance(event, i, centroidX, centroidY);
      num++;
    }

    float averageDistance = sum / num;
    return averageDistance * 2;
  }

  /**
   * Calculates the distance between the pointer given by the pointer index and the given
   * centroid.
   */
  private float calculateDistance(
    MotionEvent event, int pointerIndex, float centroidX, float centroidY) {
    PointF untransformedPoint = calculateUntransformedPoint(event, pointerIndex);

    return dist(centroidX, centroidY, untransformedPoint.x, untransformedPoint.y);
  }

  @VisibleForTesting
  static float dist(float x0, float y0, float x1, float y1) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    return (float) Math.sqrt(dx * dx + dy * dy);
  }
}
