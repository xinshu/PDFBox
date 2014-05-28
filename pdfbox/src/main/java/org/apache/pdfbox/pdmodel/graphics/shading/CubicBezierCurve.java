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
public class CubicBezierCurve
{
    protected final Point2D[] controlPoints;
    
    private final double[][] rm; // rotation matrix
    private final Point2D[] newControlPoints;
    
    /**
     * Constructor for using 4 control points of a cubic Bezier curve
     * @param ctrlPnts, [p0, p1, p2, p3]
     */
    
    public CubicBezierCurve(Point2D[] ctrlPnts)
    {
        controlPoints = ctrlPnts.clone();
        int sz = ctrlPnts.length; // always should be 4
        newControlPoints = new Point2D[sz];
        newControlPoints[0] = new Point2D.Double(0, 0);
        rm = new double[2][2];
        double tmpx = controlPoints[3].getX() - controlPoints[0].getX();
        double tmpy = controlPoints[3].getY() - controlPoints[0].getY();
        double hypotenuse = Math.sqrt(tmpx * tmpx + tmpy * tmpy);
        if (hypotenuse < 1e-2)
        {
            rm[0][0] = 1;
            rm[1][1] = 1;
        }
        else
        {
            double cosAngle = tmpx / hypotenuse, sinAngle = tmpy / hypotenuse;
            rm[0][0] = cosAngle;
            rm[0][1] = sinAngle;
            rm[1][0] = - sinAngle;
            rm[1][1] = cosAngle;
        }
        for (int i = 1; i < sz; i++)
        {
            newControlPoints[i] = rotatePoint(controlPoints[i]);
        }
    }
    
    private Point2D rotatePoint(Point2D p)
    {
        Point2D shiftp = new Point2D.Double(p.getX() - controlPoints[0].getX(), p.getY() - controlPoints[0].getY());
        double tmpx = shiftp.getX(), tmpy = shiftp.getY();
        Point2D np = new Point2D.Double(rm[0][0] * tmpx + rm[0][1] * tmpy, rm[1][0] * tmpx + rm[1][1] * tmpy);
        return np;
    }
    
    public boolean isOn(Point2D p)
    {
        Point2D np = rotatePoint(p);
        return isOn(newControlPoints, np);
    }
    
    private boolean isOn(Point2D[] ctrlPnts, Point2D p)
    {
        if (isPoint(ctrlPnts))
        {
            return Math.abs(p.getX() - ctrlPnts[0].getX()) < 1 && Math.abs(p.getY() - ctrlPnts[0].getY()) < 1;
        }
        if (p.getX() < ctrlPnts[0].getX() || p.getX() > ctrlPnts[3].getX())
        {
            return false;
        }
        boolean res;
        Point2D m01 = getMid(ctrlPnts[0], ctrlPnts[1]);
        Point2D m12 = getMid(ctrlPnts[1], ctrlPnts[2]);
        Point2D m23 = getMid(ctrlPnts[2], ctrlPnts[3]);
        Point2D m012 = getMid(m01, m12);
        Point2D m123 = getMid(m12, m23);
        Point2D m0123 = getMid(m012, m123);
        if (Math.abs(p.getX() - m0123.getX()) < 1)
        {
            res = Math.abs(p.getY() - m0123.getY()) < 1;
        }
        else if (p.getX() < m0123.getX())
        {
            Point2D[] leftSub = new Point2D[]
            {
                ctrlPnts[0], m01, m012, m0123
            };
            res = isOn(leftSub, p);
        }
        else
        {
            Point2D[] rightSub = new Point2D[]
            {
                m0123, m123, m23, ctrlPnts[3]
            };
            res = isOn(rightSub, p);
        }
        return res;
    }
    
    public boolean isAbove(Point2D p)
    {
        Point2D np = rotatePoint(p);
//        System.out.println(newControlPoints[0] + " " + newControlPoints[1] + " " + 
//                                newControlPoints[2] + " " + newControlPoints[3]);
//        System.out.println(np);
        return isAbove(newControlPoints, np);
    }
    
    public boolean isBelow(Point2D p)
    {
        Point2D np = rotatePoint(p);
        return isBelow(newControlPoints, np);
    }
    
//    public boolean isInRange(Point2D p)
//    {
//        return isInRange(newControlPoints, p);
//    }
//    
//    private boolean isInRange(Point2D[] ctrlPnts, Point2D p)
//    {
//        return (p.getX() - ctrlPnts[0].getX()) * (p.getX() - ctrlPnts[3].getX()) <= 0;
//    }
    
    public boolean isPoint()
    {
        return isPoint(newControlPoints);
    }
    
    private boolean isPoint(Point2D[] ctrlPnts)
    {
        HashSet<Point> set = new HashSet<Point>();
        for (Point2D itp : ctrlPnts)
        {
            Point np = new Point((int)itp.getX(), (int)itp.getY());
            set.add(np);
        }
        return set.size() < 2;
    }
    
    private boolean isBelow(Point2D[] ctrlPnts, Point2D p)
    {
        HashSet<Point> set = new HashSet<Point>();
    	for (Point2D itp : ctrlPnts)
    	{
            Point np = new Point((int)itp.getX() * 100, (int)itp.getY() * 100);
            set.add(np);
    	}
    	if(set.size() < 2)
    	{
            return p.getY() < ctrlPnts[0].getY();
    	}
        if (p.getX() < ctrlPnts[0].getX() || p.getX() > ctrlPnts[3].getX())
        {
            return true;
        }
        boolean res;
        Point2D m01 = getMid(ctrlPnts[0], ctrlPnts[1]);
        Point2D m12 = getMid(ctrlPnts[1], ctrlPnts[2]);
        Point2D m23 = getMid(ctrlPnts[2], ctrlPnts[3]);
        Point2D m012 = getMid(m01, m12);
        Point2D m123 = getMid(m12, m23);
        Point2D m0123 = getMid(m012, m123);
        if (Math.abs(p.getX() - m0123.getX()) < 1)
        {
            res = p.getY() < m0123.getY();
        }
        else if (p.getX() < m0123.getX())
        {
            Point2D[] leftSub = new Point2D[]
            {
                ctrlPnts[0], m01, m012, m0123
            };
            res = isBelow(leftSub, p);
        }
        else
        {
            Point2D[] rightSub = new Point2D[]
            {
                m0123, m123, m23, ctrlPnts[3]
            };
            res = isBelow(rightSub, p);
        }
        return res;
    }
    
    private boolean isAbove(Point2D[] ctrlPnts, Point2D p)
    {
        HashSet<Point> set = new HashSet<Point>();
    	for (Point2D itp : ctrlPnts)
    	{
            Point np = new Point((int)itp.getX() * 100, (int)itp.getY() * 100);
            set.add(np);
    	}
    	if(set.size() < 2)
    	{
            return p.getY() > ctrlPnts[0].getY();
    	}
        if (p.getX() < ctrlPnts[0].getX() || p.getX() > ctrlPnts[3].getX())
        {
            return true;
        }
        boolean res;
        Point2D m01 = getMid(ctrlPnts[0], ctrlPnts[1]);
        Point2D m12 = getMid(ctrlPnts[1], ctrlPnts[2]);
        Point2D m23 = getMid(ctrlPnts[2], ctrlPnts[3]);
        Point2D m012 = getMid(m01, m12);
        Point2D m123 = getMid(m12, m23);
        Point2D m0123 = getMid(m012, m123);
        if (Math.abs(p.getX() - m0123.getX()) < 1)
        {
            res = p.getY() > m0123.getY();
        }
        else if (p.getX() < m0123.getX())
        {
            Point2D[] leftSub = new Point2D[]
            {
                ctrlPnts[0], m01, m012, m0123
            };
            res = isAbove(leftSub, p);
        }
        else
        {
            Point2D[] rightSub = new Point2D[]
            {
                m0123, m123, m23, ctrlPnts[3]
            };
            res = isAbove(rightSub, p);
        }
        return res;
    }
    
    private Point2D getMid(Point2D ps, Point2D pe)
    {
        return new Point2D.Double((ps.getX() + pe.getX()) / 2, (ps.getY() + pe.getY()) / 2);
    }
    
//    public Point2D[] getCubicBezierCurve(int elementNum)
//    {
//        Point2D[] curve = new Point2D[elementNum];
//        double step = 1 / (elementNum - 1);
//        double t = - step;
//        for(int i = 0; i < elementNum; i++)
//        {
//            t += step;
//            double tmpX = (1 - t) * (1 - t)*( 1 - t) * controlPoints[0].getX() + 
//                    3 * t * (1 - t) * (1 - t) * controlPoints[1].getX() +
//                    3 * t * t * (1 - t) * controlPoints[2].getX() + 
//                    t * t * t * controlPoints[3].getX();
//            double tmpY = (1 - t) * (1 - t)*( 1 - t) * controlPoints[0].getY() + 
//                    3 * t * (1 - t) * (1 - t) * controlPoints[1].getY() +
//                    3 * t * t * (1 - t) * controlPoints[2].getY() + 
//                    t * t * t * controlPoints[3].getY();
//            curve[i] = new Point2D.Double(tmpX, tmpY);
//        }
//        return curve;
//    }
    
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
