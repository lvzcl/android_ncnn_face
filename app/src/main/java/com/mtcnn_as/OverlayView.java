/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.mtcnn_as;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * A simple View providing a render callback to other classes.
 */
public class OverlayView extends View {
  private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();

  //private final Paint paint;

  //private static final Logger LOGGER = new Logger();

  //private float resultsViewHeight;

  //private int INPUT_SIZE = 416;
  //用来保存结果
  //private List<Classifier.Recognition> results = new LinkedList<>();

  //public void setResults(final List<Classifier.Recognition> results){
  //  this.results = results;
  //  postInvalidate();
  //}


  //测试使用
  //public int getResultSize(){
    //return results.size();
  //}

  public OverlayView(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    //paint = new Paint();
    //paint.setColor(Color.BLUE);
    //paint.setStyle(Paint.Style.STROKE);
    //paint.setStrokeWidth(2.0f);
    //resultsViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
    //        112, getResources().getDisplayMetrics());
  }

  /**
   * Interface defining the callback for client classes.
   */
  public interface DrawCallback {
    public void drawCallback(final Canvas canvas);
  }

  public void addCallback(final DrawCallback callback) {
    callbacks.add(callback);
  }

  @Override
  public synchronized void draw(final Canvas canvas) {
    super.draw(canvas);
    for (final DrawCallback callback : callbacks) {
      callback.drawCallback(canvas);
    }
  }
/*
  private RectF reCalcSize(RectF rect) {
    int padding = 5;
    float overlayViewHeight = this.getHeight() - resultsViewHeight;
    float sizeMultiplier = Math.min((float) this.getWidth() / (float) INPUT_SIZE,
            overlayViewHeight / (float) INPUT_SIZE);

    float offsetX = (this.getWidth() - INPUT_SIZE * sizeMultiplier) / 2;
    float offsetY = (overlayViewHeight - INPUT_SIZE * sizeMultiplier) / 2 + resultsViewHeight;

    float left = Math.max(padding,sizeMultiplier * rect.left+ offsetX);
    float top = Math.max(offsetY + padding, sizeMultiplier * rect.top + offsetY);

    float right = Math.min(rect.right * sizeMultiplier, this.getWidth() - padding);
    float bottom = Math.min(rect.bottom * sizeMultiplier + offsetY, this.getHeight() - padding);

    return new RectF(left, top, right, bottom);
  }
*/
}
