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
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DragGestureRecognizerTests {

  private static final float E = 0.0001f;

  private View element;
  private DragGestureRecognizer dragGestureRecognizer;

  private long eventDownTime;
  private long eventTime;

  @Before
  public void setUp() {
    Context context = Robolectric.setupActivity(Activity.class);
    element = new View(context);
    dragGestureRecognizer = new DragGestureRecognizer();
    dragGestureRecognizer.dragSlop = 0;

    eventDownTime = 0;
    eventTime = -16;
  }

  @Test
  public void defaultState() {
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(dragGestureRecognizer.getElement()).isEqualTo(null);
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(0).of(0f);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(0).of(0f);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(0).of(0f);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(0).of(0f);
    assertThat(dragGestureRecognizer.getVelocityX()).isWithin(0).of(0f);
    assertThat(dragGestureRecognizer.getVelocityY()).isWithin(0).of(0f);
  }

  @Test
  public void smallMovementIsNotRecognized() {
    dragGestureRecognizer.dragSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move 1 pixel. Should not change the state.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 1, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void largeHorizontalMovementIsRecognized() {
    dragGestureRecognizer.dragSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move 100 pixel right. Should change the state.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 100, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move 1 pixel. Should still change the state.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 101, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void largeVerticalMovementIsRecognized() {
    dragGestureRecognizer.dragSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move 100 pixel right. Should change the state.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 0, 100));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move 1 pixel. Should still change the state.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 0, 101));
    assertThat(dragGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void completedGestureIsRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 100, 0));
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_UP, 100, 0));

    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test
  public void cancelledGestureIsNotRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_MOVE, 100, 0));
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_CANCEL, 100, 0));

    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CANCELLED, POSSIBLE});
  }

  @Test
  public void noMovementIsNotRecognized() {
    dragGestureRecognizer.dragSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void irrelevantMotionIsIgnored() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);

    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_HOVER_MOVE, 0, 0));

    assertThat(dragGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void multitouchHasCorrectCentroidAndTranslation() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    dragGestureRecognizer.addStateChangeListener(listener);

    // First finger down. Centroid is at finger location and translation is 0.
    dragGestureRecognizer.onTouch(element, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(E).of(0);

    // Second finger down. Centroid is in between fingers and translation is 0.
    dragGestureRecognizer.onTouch(element,
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(E).of(0);

    // Second finger moves [dx, dy]. Centroid and translation moves [dx/2, dy/2].
    float dx = 505;
    float dy = 507;
    dragGestureRecognizer.onTouch(element,
      createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50 + dx / 2);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50 + dy / 2);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(E).of(dx / 2);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(E).of(dy / 2);

    // Second finger up. Centroid is at first finger location and translation stays the same.
    dragGestureRecognizer.onTouch(element,
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(E).of(dx / 2);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(E).of(dy / 2);

    // Finger up. Centroid is at first finger location and translation is reset.
    dragGestureRecognizer.onTouch(element,
      createMotionEvent(MotionEvent.ACTION_UP, 0, 0));
    assertThat(dragGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationX()).isWithin(E).of(0);
    assertThat(dragGestureRecognizer.getTranslationY()).isWithin(E).of(0);

    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test(expected = NullPointerException.class)
  public void crashesForNullElement() {
    dragGestureRecognizer.onTouch(null, createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
  }

  @Test
  public void allowsSettingElementAgain() {
    dragGestureRecognizer.onTouch(new View(element.getContext()), createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    dragGestureRecognizer.onTouch(new View(element.getContext()), createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
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
}
