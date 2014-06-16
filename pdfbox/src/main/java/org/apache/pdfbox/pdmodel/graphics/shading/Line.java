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

import java.awt.Point;
import java.util.HashSet;

/**
 *
 * @author Shaola
 */
public class Line
{
    private final Point point0;
    private final Point point1;
    private final float[] color0;
    private final float[] color1;
    
    protected final HashSet<Point> linePoints;
    
    public Line(Point p0, Point p1, float[] c0, float[] c1)
    {
        point0 = p0;
        point1 = p1;
        color0 = c0.clone();
        color1 = c1.clone();
        linePoints = getLine();
    }
    
    private HashSet<Point> getLine()
    {
        return getLine(point0.x, point0.y, point1.x, point1.y);
    }
    
    /**
     * Bresenham's line algorithm
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return 
     */
    private HashSet<Point> getLine(int x0, int y0, int x1, int y1) 
    {
        HashSet<Point> points = new HashSet<Point>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;	
        while (true)
        {
            points.add(new Point(x0, y0));
            if (x0 == x1 && y0 == y1)
            {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy)
            {
                err = err - dy;
                x0 = x0 + sx;
            }
            if (e2 < dx)
            {
                err = err + dx;
                y0 = y0 + sy;
            }
        }
        return points;
    }
    
    // p should always be contained in linePoints
    protected float[] getColor(Point p)
    {
        int numberOfColorComponents = color0.length;
        float[] pc = new float[numberOfColorComponents];
        if (point0.x == point1.x && point0.y == point1.y)
        {
            return color0;
        }
        else if (point0.x == point1.x)
        {
            float l = point1.y - point0.y;
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pc[i] = (float) (color0[i] * (point1.y - p.y) / l + color1[i] * (p.y - point0.y) / l);
            }
        }
        else
        {
            float l = point1.x - point0.x;
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pc[i] = (float) (color0[i] * (point1.x - p.x) / l + color1[i] * (p.x - point0.x) / l);
            }
        }
        return pc;
    }
}
