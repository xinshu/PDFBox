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
import java.awt.geom.Point2D;
import java.util.HashSet;

/**
 *
 * @author Shaola
 */
public class CoonsTriangle
{
    private final Point2D[] corner;
    private final float[][] color;
    private final double area;
    
    private final int degeneracy;
    private final Line line;
    
    // edge equation value of 3 corners
    private final double v0;
    private final double v1;
    private final double v2;
    
    public CoonsTriangle(Point2D[] p, float[][] c)
    {
        corner = p.clone();
        color = c.clone();
        area = getArea(p[0], p[1], p[2]);
        //System.out.println("area: " + area);
        degeneracy = getDeg(p);
        
        if (degeneracy == 2)
        {
            if (overlaps(corner[1], corner[2]) && !overlaps(corner[0], corner[2]))
            {
                Point p0 = new Point((int)Math.round(corner[0].getX()), (int)Math.round(corner[0].getY()));
                Point p1 = new Point((int)Math.round(corner[2].getX()), (int)Math.round(corner[2].getY()));
                line = new Line(p0, p1, color[0], color[2]);
            }
            else
            {
                Point p0 = new Point((int)Math.round(corner[1].getX()), (int)Math.round(corner[1].getY()));
                Point p1 = new Point((int)Math.round(corner[2].getX()), (int)Math.round(corner[2].getY()));
                line = new Line(p0, p1, color[1], color[2]);
            }
        }
        else
        {
            line = null;
        }
        
        v0 = edgeEquationValue(p[0], p[1], p[2]);
        v1 = edgeEquationValue(p[1], p[2], p[0]);
        v2 = edgeEquationValue(p[2], p[0], p[1]);
    }
    
    private int getDeg(Point2D[] p)
    {
        HashSet<Point> set = new HashSet<Point>();
        for (Point2D itp : p)
        {
            //Point np = new Point((int)Math.round(itp.getX()), (int)Math.round(itp.getY()));
            Point np = new Point((int)Math.round(itp.getX() * 1000), (int)Math.round(itp.getY() * 1000));
            set.add(np);
        }
        return set.size();
    }
    
    public boolean contains(Point2D p)
    {
        if (degeneracy == 1)
        {
            return overlaps(corner[0], p) | overlaps(corner[1], p) | overlaps(corner[2], p);
        }
        else if (degeneracy == 2)
        {
            Point tp = new Point((int)Math.round(p.getX()), (int)Math.round(p.getY()));
            return line.linePoints.contains(tp);
//            if (overlaps(corner[0], corner[1]) && !overlaps(corner[0], corner[2]))
//            {
//                return isOnLine(corner[0], corner[2], p);
//            }
//            else
//            {
//                return isOnLine(corner[0], corner[1], p);
//            }
            
//            if (overlaps(corner[0], corner[1]) && !overlaps(corner[0], corner[2]))
//            {
//                return isOnLine(corner[1], corner[2], p);
//            }
//            else if (overlaps(corner[1], corner[2]) && !overlaps(corner[2], corner[0]))
//            {
//                return isOnLine(corner[2], corner[0], p);
//            }
//            else
//            {
//                return isOnLine(corner[2], corner[1], p);
//            }
        }
        
//        if (degeneracy < 3)
//        {
//            System.out.println("test:" + degeneracy);
//            return false;
//        }
        
        double pv0 = edgeEquationValue(p, corner[1], corner[2]);
        if (pv0 * v0 < 0)
        {
            return false;
        }
        double pv1 = edgeEquationValue(p, corner[2], corner[0]);
        if (pv1 * v1 < 0)
        {
            return false;
        }
        double pv2 = edgeEquationValue(p, corner[0], corner[1]);
        return pv2 * v2 >= 0; // !(pv2 * v2 < 0)
    }
    
    private boolean overlaps(Point2D p0, Point2D p1)
    {
        return Math.abs(p0.getX() - p1.getX()) < 0.001 && Math.abs(p0.getY() - p1.getY()) < 0.001;
    }
    
//    private boolean isOnLine(Point2D p0, Point2D p1, Point2D p)
//    {
//        if (p0.getX() > p1.getX())
//        {
//            return isOnLine(p1, p0, p);
//        }
//        if (p.getX() < p0.getX() || p.getX() > p1.getX())
//        {
//            return false;
//        }
//        else if (p0.getY() < p1.getY() && (p.getY() < p0.getY() || p.getY() > p1.getY()))
//        {
//            return false;
//        }
//        else if (p1.getY() < p0.getY() && (p.getY() < p1.getY() || p.getY() > p0.getY()))
//        {
//            return false;
//        }
//        double x = p1.getX() - p0.getX(), y = p1.getY() - p0.getY();
//        double dx = p.getX() - p0.getX(), dy = p.getY() - p0.getY();
//        return Math.abs(y * dx - dy * x) <= Math.abs(x) || Math.abs(x * dy - y * dx) <= Math.abs(y);
//    }
    
//    private double getDis(Point2D p0, Point2D p1)
//    {
//        double x = p1.getX() - p0.getX();
//        double y = p1.getY() - p0.getY();
//        return Math.sqrt(x * x + y * y);
//    }
    
    private double edgeEquationValue(Point2D p, Point2D p1, Point2D p2)
    {
        return (p2.getY() - p1.getY()) * (p.getX() - p1.getX()) - (p2.getX() - p1.getX()) * (p.getY() - p1.getY());
    }
    
    private double getArea(Point2D a, Point2D b, Point2D c)
    {
        return Math.abs((c.getX() - b.getX()) * (c.getY() - a.getY()) - (c.getX() - a.getX()) * (c.getY() - b.getY())) / 2.0;
    }
    
    public float[] getColor(Point2D p)
    {
        int numberOfColorComponents = color[0].length;
        float[] pCol = new float[numberOfColorComponents];
        
        if (degeneracy == 1)
        {
            //pCol = color[2];
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                pCol[i] = (color[0][i] + color[1][i] + color[2][i]) / 3.0f;
            }
        }
        else if (degeneracy == 2)
        {
            Point tp = new Point((int)Math.round(p.getX()), (int)Math.round(p.getY()));
            return line.getColor(tp);
//            if (overlaps(corner[1], corner[2]) && !overlaps(corner[0], corner[2]))
//            {
//                pCol = getColorOnALine(p, corner[0], corner[2], color[0], color[2]);
//            }
//            else
//            {
//                pCol = getColorOnALine(p, corner[1], corner[2], color[1], color[2]);
//            }
            
//            if (overlaps(corner[0], corner[1]) && !overlaps(corner[0], corner[2]))
//            {
//                pCol = getColorOnALine(p, corner[1], corner[2], color[1], color[2]);
//            }
//            else if (overlaps(corner[1], corner[2]) && !overlaps(corner[2], corner[0]))
//            {
//                pCol = getColorOnALine(p, corner[2], corner[0], color[2], color[0]);
//            }
//            else
//            {
//                pCol = getColorOnALine(p, corner[2], corner[1], color[2], color[1]);
//            }
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
    
//    private float[] getColorOnALine(Point2D p, Point2D a, Point2D b, float[] ac, float[] bc)
//    {
//        int numberOfColorComponents = ac.length;
//        float[] pc = new float[numberOfColorComponents];
//        if (Math.abs(a.getX() - b.getX()) < 0.001)
//        {
//            double l = b.getY() - a.getY();
//            for (int i = 0; i < numberOfColorComponents; i++)
//            {
//                pc[i] = (float) (ac[i] * (b.getY() - p.getY()) / l + bc[i] * (p.getY() - a.getY()) / l);
//            }
//        }
//        else
//        {
//            double l = b.getX() - a.getX();
//            for (int i = 0; i < numberOfColorComponents; i++)
//            {
//                pc[i] = (float) (ac[i] * (b.getX() - p.getX()) / l + bc[i] * (p.getX() - a.getX()) / l);
//            }
//        }
//        return pc;
//    }
    
    @Override
    public String toString()
    {
       return corner[0] + " " + corner[1] + " " + corner[2];
    }
}
