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

/**
 *
 * @author Shaola
 */
public class CoonsPatch
{
    protected final CubicBezierCurve edgeC1;
    protected final CubicBezierCurve edgeC2;
    protected final CubicBezierCurve edgeD1;
    protected final CubicBezierCurve edgeD2;
    protected final float[][] cornerColor;
    
    private boolean isClockwise;
    
//    private final int horizontalElementNum;
//    private final int verticalElementNum;
//    private final double stepu;
//    private final double stepv;
//    
//    private final Point2D[] curveC1;
//    private final Point2D[] curveC2;
//    private final Point2D[] curveD1;
//    private final Point2D[] curveD2; 
    
    /**
     * Constructor for using 4 edges and color of 4 corners
     * @param C1 edge C1 of the coons patch, contains 4 control points [p10, p11, p12, p1]
     * @param C2 edge C2 of the coons patch, contains 4 control points [p4, p5, p6, p7]
     * @param D1 edge D1 of the coons patch, contains 4 control points [p1, p2, p3, p4]
     * @param D2 edge D2 of the coons patch, contains 4 control points [p7, p8, p9, p10]
     * @param color 4 corner colors, value is as [c1, c2, c3, c4]
     */
    public CoonsPatch(CubicBezierCurve C1, CubicBezierCurve C2, CubicBezierCurve D1, CubicBezierCurve D2, 
                                float[][] color)
    {
        edgeC1 = C1;
        edgeC2 = C2;
        edgeD1 = D1;
        edgeD2 = D2;
        cornerColor = color.clone();
        
        Point2D p0 = D1.controlPoints[0];
        Point2D p1 = D1.controlPoints[3];
        Point2D p2 = D2.controlPoints[0];
        Point2D p3 = D2.controlPoints[3];
        
        // need to rewrite
        isClockwise = (wiseNum(p0, p1, p2) == 1 || wiseNum(p0, p2, p3) == 1);
        
//        int h1 = (int) Math.abs(edgeC1.controlPoints[0].getX() - edgeC1.controlPoints[3].getX());
//        int h2 = (int) Math.abs(edgeC2.controlPoints[0].getX() - edgeC2.controlPoints[3].getX());
//        int v1 = (int) Math.abs(edgeD1.controlPoints[0].getY() - edgeD1.controlPoints[3].getY());
//        int v2 = (int) Math.abs(edgeD2.controlPoints[0].getY() - edgeD2.controlPoints[3].getY());
        
//        horizontalElementNum = Math.min(Math.max(h1, h2), 10) + 2;
//        verticalElementNum = Math.min(Math.max(v1, v2), 10) + 2;
//        
//        stepu = 1 / (horizontalElementNum - 1);
//        stepv = 1 / (verticalElementNum - 1);
//        
//        curveC1 = edgeC1.getCubicBezierCurve(horizontalElementNum);
//        curveC2 = edgeC2.getCubicBezierCurve(horizontalElementNum);
//        curveD1 = edgeD1.getCubicBezierCurve(verticalElementNum);
//        curveD2 = edgeD2.getCubicBezierCurve(verticalElementNum);
    }
    
    public boolean contains(Point2D p)
    {
        CubicBezierCurve[] C = {edgeC1, edgeC2, edgeD1, edgeD2};
        return contains(C, p);
    }
    
    private boolean contains(CubicBezierCurve[] C, Point2D p)
    {
        int sz = C.length;
        boolean[] isPointC = new boolean[sz];
        int cnt = 0;
        for (int i = 0; i < sz; i++)
        {
            isPointC[i] = C[i].isPoint();
            if (isPointC[i])
            {
                cnt++;
            }
        }
        if (cnt > 2)
        {
            return false;
        }
        
        boolean res = true;
        
        if (isClockwise)
        {
            for (int i = 0; i < sz; i++)
            {
                if (!isPointC[i])
                {
                    res &= !C[i].isBelow(p);
                }
                if (!res)
                {
                    return false;
                }
            }
        }
        else
        {
            for (int i = 0; i < sz; i++)
            {
                if (!isPointC[i])
                {
                    res &= (!C[i].isAbove(p));
                }
                if (!res)
                {
                    return false;
                }
            }
        }
        return res;
    }
    
//    private boolean isAboveLine(Point2D p0, Point2D p1, Point2D p)
//    {
//        if ((p0.getX() - p1.getX()) < 1e-2)
//        {
//            return p.getX() < p0.getX();
//        }
//        return p.getY() > (p0.getY() + (p.getX() - p0.getX()) * (p1.getY() - p0.getY()) / (p1.getX() - p0.getX()));
//    }
    
    private int wiseNum(Point2D p0, Point2D p1, Point2D p2)
    {
        double x0 = p0.getX(), y0 = p0.getY();
        Point2D v1 = new Point2D.Double(p1.getX() - x0, p1.getY() - y0);
        Point2D v2 = new Point2D.Double(p2.getX() - x0, p2.getY() - y0);
        double product = v1.getX() * v2.getY() - v2.getX() * v1.getY();
        int res;
        if (product < 1e-2)
        {
            res = 0;
        }
        else if (product > 0)
        {
            res = 1;
        }
        else
        {
            res = -1;
        }
        return res;
    }
    
//    public float[] getParamSpaceColor(int i, int j)
//    {
//        int numberOfColorComponents = cornerColor[0].length;
//        float[] paramSC = new float[numberOfColorComponents];
//        
//        double u = stepu * j;
//        double v = stepv * i;
//        for(int ci = 0; ci < numberOfColorComponents; ci++)
//        {
//            paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
//                    + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
//        }
//        return paramSC;
//    }
//    
//    public Point2D[][] getPatchCoordinates()
//    {
//        Point2D[][] patchCo = new Point2D[verticalElementNum][horizontalElementNum];
//        
//        double v = -stepv;
//        for(int i = 0; i < verticalElementNum; i++)
//        {
//            v += stepv;
//            double u = -stepu;
//            for(int j = 0; j < horizontalElementNum; j++)
//            {
//                u += stepu;
//                double scx = (1 - v) * curveC1[j].getX() + v * curveC2[j].getX();
//                double scy = (1 - v) * curveC1[j].getY() + v * curveC2[j].getY();
//                double sdx = (1 - u) * curveD1[i].getX() + u * curveD2[i].getX();
//                double sdy = (1 - u) * curveD1[i].getY() + u * curveD2[i].getY();
//                double sbx = (1 - v) * ((1 - u) * edgeC1.controlPoints[0].getX() 
//                        + u * edgeC1.controlPoints[3].getX()) 
//                        + v * ((1 - u) * edgeC2.controlPoints[0].getX() 
//                        + u * edgeC2.controlPoints[3].getX());
//                double sby = (1 - v) * ((1 - u) * edgeC1.controlPoints[0].getY() 
//                        + u * edgeC1.controlPoints[3].getY()) 
//                        + v * ((1 - u) * edgeC2.controlPoints[0].getY() 
//                        + u * edgeC2.controlPoints[3].getY());
//                
//                double sx = scx + sdx - sbx;
//                double sy = scy + sdy - sby;
//                
//                patchCo[i][j] = new Point2D.Double(sx, sy);
//            }
//        }
//        return patchCo;
//    }
    
    @Override
    public String toString()
    {
        String colorStr = "";
        
        for (float[] cornerColor1 : cornerColor)
        {
            for (float f : cornerColor1)
            {
                if (!colorStr.isEmpty())
                {
                    colorStr += " ";
                }
                colorStr += String.format("%3.2f", f);
            }
            colorStr += "\n";
        }
        
        return "CoonsPatch {edge C1 = [" + edgeC1.toString() + "]\n" 
                + "edge C2 = [" + edgeC2.toString() + "]\n"
                + "edge D1 = [" + edgeD1.toString() + "]\n"
                + "edge D2 = [" + edgeD2.toString() + "]\n"
                + " colors=[" + colorStr + "] }";
    }
}
