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
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import static com.google.android.material.motion.gestures.ValueVelocityTracker.ADDITIVE;

/**
 * A gesture recognizer that generates translation events.
 */
public class DragGestureRecognizer extends GestureRecognizer {

  /**
   * Touch slop for drag. Amount of pixels that the centroid needs to move in either axes.
   */
  public int dragSlop = UNSET_SLOP;

  private float initialCentroidX;
  private float initialCentroidY;
  private float currentCentroidX;
  private float currentCentroidY;

  private ValueVelocityTracker centroidXVelocityTracker;
  private ValueVelocityTracker centroidYVelocityTracker;

  @Override
  public void setElement(@Nullable View element) {
    super.setElement(element);

    if (element == null) {
      return;
    }

    if (dragSlop == UNSET_SLOP) {
      Context context = element.getContext();
      this.dragSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    centroidXVelocityTracker = new ValueVelocityTracker(element.getContext(), ADDITIVE);
    centroidYVelocityTracker = new ValueVelocityTracker(element.getContext(), ADDITIVE);
  }

  public boolean onTouchEvent(MotionEvent event) {
    PointF centroid = calculateUntransformedCentroid(event);
    float centroidX = centroid.x;
    float centroidY = centroid.y;

    int action = MotionEventCompat.getActionMasked(event);
    if (action == MotionEvent.ACTION_DOWN) {
      initialCentroidX = centroidX;
      initialCentroidY = centroidY;
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      centroidXVelocityTracker.onGestureStart(event, centroidX);
      centroidYVelocityTracker.onGestureStart(event, centroidY);

      if (dragSlop == 0) {
        setState(BEGAN);
      }
    }
    if (action == MotionEvent.ACTION_POINTER_DOWN
      || action == MotionEvent.ACTION_POINTER_UP) {
      float adjustX = centroidX - currentCentroidX;
      float adjustY = centroidY - currentCentroidY;

      initialCentroidX += adjustX;
      initialCentroidY += adjustY;
      currentCentroidX += adjustX;
      currentCentroidY += adjustY;

      centroidXVelocityTracker.onGestureAdjust(-adjustX);
      centroidYVelocityTracker.onGestureAdjust(-adjustY);
    }
    if (action == MotionEvent.ACTION_MOVE) {
      if (!isInProgress()) {
        float deltaX = centroidX - initialCentroidX;
        float deltaY = centroidY - initialCentroidY;
        if (Math.abs(deltaX) > dragSlop || Math.abs(deltaY) > dragSlop) {
          float adjustX = Math.signum(deltaX) * Math.min(Math.abs(deltaX), dragSlop);
          float adjustY = Math.signum(deltaY) * Math.min(Math.abs(deltaY), dragSlop);

          initialCentroidX += adjustX;
          initialCentroidY += adjustY;
          currentCentroidX += adjustX;
          currentCentroidY += adjustY;

          setState(BEGAN);
        }
      }

      if (isInProgress()) {
        currentCentroidX = centroidX;
        currentCentroidY = centroidY;

        setState(CHANGED);
      }

      centroidXVelocityTracker.onGestureMove(event, centroidX);
      centroidYVelocityTracker.onGestureMove(event, centroidY);
    }
    if (action == MotionEvent.ACTION_UP
      || action == MotionEvent.ACTION_CANCEL) {
      initialCentroidX = centroidX;
      initialCentroidY = centroidY;
      currentCentroidX = centroidX;
      currentCentroidY = centroidY;

      centroidXVelocityTracker.onGestureEnd(event, centroidX);
      centroidYVelocityTracker.onGestureEnd(event, centroidY);

      if (isInProgress()) {
        if (action == MotionEvent.ACTION_UP) {
          setState(RECOGNIZED);
        } else {
          setState(CANCELLED);
        }
      }
    }

    return true;
  }

  /**
   * Returns the translationX of the drag gesture.
   * <p>
   * This reports the total translation over time since the {@link #BEGAN beginning} of the
   * gesture. This is not a delta value from the last {@link #CHANGED update}.
   */
  public float getTranslationX() {
    return currentCentroidX - initialCentroidX;
  }

  /**
   * Returns the translationY of the drag gesture.
   * <p>
   * This reports the total translation over time since the {@link #BEGAN beginning} of the
   * gesture. This is not a delta value from the last {@link #CHANGED update}.
   */
  public float getTranslationY() {
    return currentCentroidY - initialCentroidY;
  }

  /**
   * Returns the positional velocityX of the drag gesture.
   * <p>
   * Only read this when the state is {@link #RECOGNIZED} or {@link #CANCELLED}.
   *
   * @return The velocity in pixels per second.
   */
  public float getVelocityX() {
    return centroidXVelocityTracker.getCurrentVelocity();
  }

  /**
   * Returns the positional velocityY of the drag gesture.
   * <p>
   * Only read this when the state is {@link #RECOGNIZED} or {@link #CANCELLED}.
   *
   * @return The velocity in pixels per second.
   */
  public float getVelocityY() {
    return centroidYVelocityTracker.getCurrentVelocity();
  }

  @Override
  public float getUntransformedCentroidX() {
    return currentCentroidX;
  }

  @Override
  public float getUntransformedCentroidY() {
    return currentCentroidY;
  }
}
