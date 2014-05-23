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
public class Triangle
{
    private final Point[] corner;
    private final float[][] color;
    private final boolean[] containsEdge;
    private final double area;
    
    public Triangle(Point[] p, float[][] c, boolean[] edge)
    {
        corner = p.clone();
        color = c.clone();
        containsEdge = edge.clone();
        area = getArea(p[0], p[1], p[2]);
    }
    
    // cross product of two vectors
    private double getArea(Point a, Point b, Point c)
    {
        return Math.abs((c.x - b.x) * (c.y - a.y) - (c.x - a.x) * (c.y - b.y)) / 2.0;
    }
    
    public float[] getColor(Point p)
    {
        int numberOfColorComponents = color[0].length;
        float[] pCol = new float[numberOfColorComponents];
        if (area == 0)
        {
            if (corner[0].equals(corner[1]) && !corner[0].equals(corner[2]))
            {
                pCol = getColorOnALine(p, corner[0], corner[2], color[0], color[2]);
            }
            else if (corner[0].equals(corner[2]) && !corner[0].equals(corner[1]))
            {
                pCol = getColorOnALine(p, corner[0], corner[1], color[0], color[1]);
            }
        }
        else
        {
            float aw = (float) (getArea(p, corner[1], corner[2]) / area);
            float bw = (float) (getArea(p, corner[2], corner[0]) / area);
            float cw = (float) (getArea(p, corner[0], corner[1]) / area);
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pCol[i] = color[0][i] * aw + color[1][i] * bw + color[2][i] * cw;
            }
        }
        return pCol;
    }
    
    private float[] getColorOnALine(Point p, Point a, Point b, float[] ac, float[] bc)
    {
        int numberOfColorComponents = ac.length;
        float[] pc = new float[numberOfColorComponents];
        if (a.x == b.x)
        {
            int l = b.y - a.y;
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pc[i] = ac[i] * (b.y - p.y) / l + bc[i] * (p.y - a.y) / l;
            }
        }
        else
        {
            int l = b.x - a.x;
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pc[i] = ac[i] * (b.x - p.x) / l + bc[i] * (p.x - a.x) / l;
            }
        }
        return pc;
    }
    
    public HashSet<Point> getPointsInATriangle()
    {
        return getPointsInATriangle(corner[0], corner[1], corner[2], containsEdge);
    }
    
    private HashSet<Point> getPointsInATriangle(Point A, Point B, Point C, boolean[] contains)
    {
        HashSet<Point> insidePoints = new HashSet<Point>();
        HashSet<Point> baseLine = getPointsOnALine(B, C);
        if (contains[1])
        {
            insidePoints.addAll(baseLine);
        }
        if (contains[0])
        {
            insidePoints.addAll(getPointsOnALine(A, B));
        }
        if (contains[2])
        {
            insidePoints.addAll(getPointsOnALine(C, A));
        }
        
        HashSet<Integer> count = new HashSet<Integer>();
        for (Point p : baseLine)
        {
            if (!count.contains(p.x))
            {
                insidePoints.addAll(getPointsOnALine(A, p));
                count.add(p.x);
            }
        }
        return insidePoints;
    }
    
    private HashSet<Point> getPointsOnALine(Point A, Point B)
    {
        HashSet<Point> points = new HashSet<Point>();
        if (A.equals(B))
        {
        }
        else if (A.x == B.x)
        {
            if (A.y < B.y)
            {
                for (int i = A.y + 1; i < B.y; i++)
                {
                    points.add(new Point(A.x, i));
                }
            }
            else
            {
                for (int i = B.y + 1; i < A.y; i++)
                {
                    points.add(new Point(A.x, i));
                }
            }
        }
        else if (A.y == B.y)
        {
            if (A.x < B.x)
            {
                for (int i = A.x + 1; i < B.x; i++)
                {
                    points.add(new Point(i, A.y));
                }
            }
            else
            {
                for (int i = B.x + 1; i < A.x; i++)
                {
                    points.add(new Point(i, A.y));
                }
            }
        }
        else
        {
            double k = (double) (B.y - A.y) / (B.x - A.x);
            if (A.x < B.x)
            {
                for (int i = A.x + 1; i < B.x; i++)
                {
                    int yl = (int) Math.round((double) A.y - 0.5 + k * (i - 0.5 - A.x));
                    int yh = (int) Math.round((double) A.y + 0.5 + k * (i + 0.5 - A.x));
                    for (int j = yl; j <= yh; j++)
                    {
                        points.add(new Point(i, j));
                    }
                }
            }
            else
            {
                for (int i = B.x + 1; i < A.x; i++)
                {
                    int yl = (int) Math.round((double) B.y - 0.5 + k * (i - 0.5 - B.x));
                    int yh = (int) Math.round((double) B.y + 0.5 + k * (i + 0.5 - B.x));
                    for (int j = yl; j <= yh; j++)
                    {
                        points.add(new Point(i, j));
                    }
                }
            }
        }
        return points;
    }
}
