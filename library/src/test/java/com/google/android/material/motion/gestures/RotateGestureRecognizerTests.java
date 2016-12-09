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

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.android.material.motion.gestures.GestureRecognizer.BEGAN;
import static com.google.android.material.motion.gestures.GestureRecognizer.CANCELLED;
import static com.google.android.material.motion.gestures.GestureRecognizer.CHANGED;
import static com.google.android.material.motion.gestures.GestureRecognizer.POSSIBLE;
import static com.google.android.material.motion.gestures.GestureRecognizer.RECOGNIZED;
import static com.google.android.material.motion.gestures.RotateGestureRecognizer.angle;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class RotateGestureRecognizerTests {

  private static final float E = 0.0001f;

  private View element;
  private RotateGestureRecognizer rotateGestureRecognizer;

  private long eventDownTime;
  private long eventTime;

  @Before
  public void setUp() {
    Context context = Robolectric.setupActivity(Activity.class);
    element = new View(context);
    rotateGestureRecognizer = new RotateGestureRecognizer();
    rotateGestureRecognizer.setElement(element);
    rotateGestureRecognizer.rotateSlop = 0;

    eventDownTime = 0;
    eventTime = -16;
  }

  @Test
  public void defaultState() {
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(rotateGestureRecognizer.getElement()).isEqualTo(element);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidX()).isWithin(0).of(0f);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidY()).isWithin(0).of(0f);
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(0).of(0f);
    assertThat(rotateGestureRecognizer.getVelocity()).isWithin(0).of(0f);
  }

  @Test
  public void smallMovementIsNotRecognized() {
    rotateGestureRecognizer.rotateSlop = (float) (Math.PI / 4); // 45 degrees.

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger up less than 45 degrees. Should not change the state.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 99));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void largeCounterClockwiseMovementIsRecognized() {
    rotateGestureRecognizer.rotateSlop = (float) (Math.PI / 4); // 45 degrees.

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger up more than 45 degrees. Should change the state.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 101));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move second finger 1 pixel. Should still change the state.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 102));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void largeClockwiseMovementIsRecognized() {
    rotateGestureRecognizer.rotateSlop = (float) (Math.PI / 4); // 45 degrees.

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger down more than 45 degrees. Should change the state.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, -101));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move second finger 1 pixel. Should still change the state.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, -102));
    assertThat(rotateGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void completedGestureIsRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, 0, 0, 100, 100, 200, 200));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 200, 100, 200, 200));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 2, 0, 0, 200, 100, 200, 200));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 200, 100));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test
  public void cancelledOneFingerGestureIsNotRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_MOVE, 100, 0));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_CANCEL, 100, 0));

    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void cancelledTwoFingerGestureIsNotRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 200, 100));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_CANCEL, 0, 0, 0, 200, 100));

    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CANCELLED, POSSIBLE});
  }

  @Test
  public void noMovementIsNotRecognized() {
    rotateGestureRecognizer.rotateSlop = (float) (Math.PI / 4); // 45 degrees.

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100, 100));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void irrelevantMotionIsIgnored() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);

    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_HOVER_MOVE, 0, 0));

    assertThat(rotateGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void oneFingerDoesNotAffectRotate() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);

    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0f);

    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_MOVE, 100, 100));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0f);
  }

  @Test
  public void multitouchHasCorrectCentroidAndRotation() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);

    // First finger down. Centroid is at finger location and rotation is 0.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Second finger down. Centroid is in between fingers and rotation is 1.
    rotateGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(rotateGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50);
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Second finger moves [dx, dy]. Centroid moves [dx/2, dy/2], rotation is calculated correctly.
    float dx = 5;
    float dy = 507;
    rotateGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(rotateGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50 + dx / 2);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50 + dy / 2);
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(
      angle(0, 0, 100 + dx, 100 + dy) - angle(0, 0, 100, 100));

    // Second finger up. State is now reset.
    rotateGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(rotateGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(rotateGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    assertThat(listener.states.toArray()).isEqualTo(
      new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test
  public void thirdFingerDoesNotAffectRotation() {
    // First finger.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Second finger on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Third finger also on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, 0, 0, 100, 0, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Move third finger.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 2, 0, 0, 100, 0, 200, 200));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);
  }

  @Test
  public void rotationIsStableOnFirstFingerUp() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    rotateGestureRecognizer.addStateChangeListener(listener);

    // First finger down.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Second finger down on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Third finger also down on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, 0, 0, 100, 0, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Move second finger 45 degrees.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 100, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of((float) (Math.PI / 4));

    // First finger up. Rotation stays the same.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 0, 0, 0, 100, 100, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of((float) (Math.PI / 4));
  }

  @Test
  public void rotationIsStableOnSecondFingerUp() {
    // First finger down.
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Second finger down on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Third finger also down on horizontal line.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, 0, 0, 100, 0, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of(0);

    // Move second finger 45 degrees.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 100, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of((float) (Math.PI / 4));

    // Second finger up. Rotation stays the same.
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100, 100, 200, 0));
    assertThat(rotateGestureRecognizer.getRotation()).isWithin(E).of((float) (Math.PI / 4));
  }

  @Test
  public void nonZeroVelocity() {
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 10, 0));

    float move = 0;
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 10, 0 + (move += 10)));
    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 10, 0 + (move += 10)));

    rotateGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 10 + move, 0));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(rotateGestureRecognizer.getVelocity()).isGreaterThan(0f);
  }

  @Test(expected = NullPointerException.class)
  public void crashesForNullElement() {
    rotateGestureRecognizer.setElement(null);
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
  }

  @Test
  public void allowsSettingElementAgain() {
    rotateGestureRecognizer.setElement(new View(element.getContext()));
    rotateGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
  }

  private MotionEvent createMotionEvent(int action, float x, float y) {
    return MotionEvent.obtain(eventDownTime, eventTime += 16, action, x, y, 0);
  }

  private MotionEvent createMultiTouchMotionEvent(
    int action, int index, float x0, float y0, float x1, float y1) {
    MotionEvent event = mock(MotionEvent.class);

    when(event.getDownTime()).thenReturn(eventDownTime);
    when(event.getEventTime()).thenReturn(eventTime += 16);

    when(event.getPointerCount()).thenReturn(2);
    when(event.getAction()).thenReturn(action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    when(event.getActionMasked()).thenReturn(action);
    when(event.getActionIndex()).thenReturn(index);

    when(event.getRawX()).thenReturn(x0);
    when(event.getRawY()).thenReturn(y0);

    when(event.getX(0)).thenReturn(x0);
    when(event.getY(0)).thenReturn(y0);

    when(event.getX(1)).thenReturn(x1);
    when(event.getY(1)).thenReturn(y1);

    return event;
  }

  private MotionEvent createMultiTouchMotionEvent(
    int action, int index, float x0, float y0, float x1, float y1, float x2, float y2) {
    MotionEvent event = mock(MotionEvent.class);

    when(event.getDownTime()).thenReturn(eventDownTime);
    when(event.getEventTime()).thenReturn(eventTime += 16);

    when(event.getPointerCount()).thenReturn(3);
    when(event.getAction()).thenReturn(action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    when(event.getActionMasked()).thenReturn(action);
    when(event.getActionIndex()).thenReturn(index);

    when(event.getRawX()).thenReturn(x0);
    when(event.getRawY()).thenReturn(y0);

    when(event.getX(0)).thenReturn(x0);
    when(event.getY(0)).thenReturn(y0);

    when(event.getX(1)).thenReturn(x1);
    when(event.getY(1)).thenReturn(y1);

    when(event.getX(2)).thenReturn(x2);
    when(event.getY(2)).thenReturn(y2);

    return event;
  }
}
