/*
 * Copyright 2014 The Apache Software Foundation.
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

package org.apache.pdfbox.pdmodel.graphics.shading;

import java.awt.geom.Point2D;

/**
 *
 * @author Shaola
 */
public class CubicBezierCurve
{
    protected final Point2D[] controlPoints;
    
    /**
     * Constructor for using 4 control points of a cubic Bezier curve
     * @param ctlPnts, [p0, p1, p2, p3]
     */
    
    public CubicBezierCurve(Point2D[] ctlPnts)
    {
        controlPoints = ctlPnts.clone();
    }
    
    public Point2D[] getCubicBezierCurve(int elementNum)
    {
        Point2D[] curve = new Point2D[elementNum];
        double step = 1 / (elementNum - 1);
        double t = - step;
        for(int i = 0; i < elementNum; i++)
        {
            t += step;
            double tmpX = (1 - t) * (1 - t)*( 1 - t) * controlPoints[0].getX() + 
                    3 * t * (1 - t) * (1 - t) * controlPoints[1].getX() +
                    3 * t * t * (1 - t) * controlPoints[2].getX() + 
                    t * t * t * controlPoints[3].getX();
            double tmpY = (1 - t) * (1 - t)*( 1 - t) * controlPoints[0].getY() + 
                    3 * t * (1 - t) * (1 - t) * controlPoints[1].getY() +
                    3 * t * t * (1 - t) * controlPoints[2].getY() + 
                    t * t * t * controlPoints[3].getY();
            curve[i] = new Point2D.Double(tmpX, tmpY);
        }
        return curve;
    }
    
    @Override
    public String toString()
    {
        String pointStr = "";
        for (Point2D p : controlPoints)
        {
            if (!pointStr.isEmpty())
            {
                pointStr += " ";
            }
            pointStr += p;
        }
        return "Cubic Bezier curve{control points p0, p1, p2, p3: " + pointStr + "}";
    }
}