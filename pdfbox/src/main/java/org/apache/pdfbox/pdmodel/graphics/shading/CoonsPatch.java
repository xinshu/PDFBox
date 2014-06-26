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
import java.util.ArrayList;

/**
 * This class is used to describe a patch for type 6 shading.
 * This was done as part of GSoC2014.
 * @author Shaola Ren
 */
class CoonsPatch
{
    protected final Point2D[] edgeC1;
    protected final Point2D[] edgeC2;
    protected final Point2D[] edgeD1;
    protected final Point2D[] edgeD2;
    protected final float[][] cornerColor;
    
    private final int[] level; // {levelU, levelV}
    
    protected final ArrayList<CoonsTriangle> listOfCoonsTriangle;
    
    /**
     * Constructor for using 4 edges and color of 4 corners
     * @param C1 edge C1 of the coons patch, contains 4 control points [p1, p12, p11, p10] not [p10, p11, p12, p1]
     * @param C2 edge C2 of the coons patch, contains 4 control points [p4, p5, p6, p7]
     * @param D1 edge D1 of the coons patch, contains 4 control points [p1, p2, p3, p4]
     * @param D2 edge D2 of the coons patch, contains 4 control points [p10, p9, p8, p7] not [p7, p8, p9, p10]
     * @param color 4 corner colors, value is as [c1, c2, c3, c4]
     */
    public CoonsPatch(Point2D[] C1, Point2D[] C2, Point2D[] D1, Point2D[] D2, float[][] color)
    {
        edgeC1 = C1.clone();
        edgeC2 = C2.clone();
        edgeD1 = D1.clone();
        edgeD2 = D2.clone();
        cornerColor = color.clone();
        
        level = setLevel();
        
        listOfCoonsTriangle = getCoonsTriangle(level);
    }
    
    private int[] setLevel()
    {
        int[] l = {4, 4};
        if (isEdgeALine(edgeC1) & isEdgeALine(edgeC2))
        {
            double lc1 = getLen(edgeC1[0], edgeC1[3]), lc2 = getLen(edgeC2[0], edgeC2[3]);
            if (lc1 > 800 || lc2 > 800)
            {
                l[0] = 4;
            }
            else if (lc1 > 400 || lc2 > 400)
            {
                l[0] = 3;
            }
            else if (lc1 > 200 || lc2 > 200)
            {
                l[0] = 2;
            }
            else
            {
                l[0] = 1;
            }
        }
        if (isEdgeALine(edgeD1) & isEdgeALine(edgeD2))
        {
            double ld1 = getLen(edgeD1[0], edgeD1[3]), ld2 = getLen(edgeD2[0], edgeD2[3]);
            if (ld1 > 800 || ld2 > 800)
            {
                l[1] = 4;
            }
            else if (ld1 > 400 || ld2 > 400)
            {
                l[1] = 3;
            }
            else if (ld1 > 200 || ld2 > 200)
            {
                l[1] = 2;
            }
            else
            {
                l[1] = 1;
            }
        }
        return l;
    }
    
    private double getLen(Point2D ps, Point2D pe)
    {
        double x = pe.getX() - ps.getX();
        double y = pe.getY() - ps.getY();
        return Math.sqrt(x * x + y * y);
    }
    
    private boolean isEdgeALine(Point2D[] ctl)
    {
        double ctl1 = Math.abs(edgeEquationValue(ctl[1], ctl[0], ctl[3]));
        double ctl2 = Math.abs(edgeEquationValue(ctl[2], ctl[0], ctl[3]));
        double x = Math.abs(ctl[0].getX() - ctl[3].getX());
        double y = Math.abs(ctl[0].getY() - ctl[3].getY());
        return (ctl1 <= x && ctl2 <= x) || (ctl1 <= y && ctl2 <= y);
    }
    
    private double edgeEquationValue(Point2D p, Point2D p1, Point2D p2)
    {
        return (p2.getY() - p1.getY()) * (p.getX() - p1.getX()) - (p2.getX() - p1.getX()) * (p.getY() - p1.getY());
    }
    
    private Point2D getMid(Point2D ps, Point2D pe)
    {
        return new Point2D.Double((ps.getX() + pe.getX()) / 2, (ps.getY() + pe.getY()) / 2);
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle(int[] l)
    {
        CubicBezierCurve eC1 = new CubicBezierCurve(edgeC1, l[0]);
        CubicBezierCurve eC2 = new CubicBezierCurve(edgeC2, l[0]);
        CubicBezierCurve eD1 = new CubicBezierCurve(edgeD1, l[1]);
        CubicBezierCurve eD2 = new CubicBezierCurve(edgeD2, l[1]);
        return getCoonsTriangle(eC1, eC2, eD1, eD2);
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle(CubicBezierCurve C1, CubicBezierCurve C2, CubicBezierCurve D1, CubicBezierCurve D2)
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor(C1, C2, D1, D2); // at least 2 x 2, always square
        int szV = patchCC.length;
        int szU = patchCC[0].length;
        for (int i = 1; i < szV; i++)
        {
            for (int j = 1; j < szU; j++)
            {
                Point2D p0 = patchCC[i-1][j-1].coordinate, p1 = patchCC[i-1][j].coordinate, p2 = patchCC[i][j].coordinate, 
                                p3 = patchCC[i][j-1].coordinate;
                boolean ll = true;
                if (overlaps(p0, p1) || overlaps(p0, p3))
                {
                    ll = false;
                }
                else{
                    Point2D[] llCorner = {p0, p1, p3}; // counter clock wise
                    float[][] llColor = {patchCC[i-1][j-1].color, patchCC[i-1][j].color, patchCC[i][j-1].color};
                    CoonsTriangle tmpll = new CoonsTriangle(llCorner, llColor); // lower left triangle
                    list.add(tmpll);
                }
                if (ll && (overlaps(p2, p1) || overlaps(p2, p3)))
                {
                }
                else
                {
                    Point2D[] urCorner = {p3, p1, p2}; // counter clock wise
                    float[][] urColor = {patchCC[i][j-1].color, patchCC[i-1][j].color, patchCC[i][j].color};
                    CoonsTriangle tmpur = new CoonsTriangle(urCorner, urColor); // upper right triangle
                    list.add(tmpur);
                }
            }
        }
        return list;
    }
    
    private boolean overlaps(Point2D p0, Point2D p1)
    {
        return Math.abs(p0.getX() - p1.getX()) < 0.001 && Math.abs(p0.getY() - p1.getY()) < 0.001;
    }
    
    private CoordinateColorPair[][] getPatchCoordinatesColor(CubicBezierCurve C1, CubicBezierCurve C2, CubicBezierCurve D1, CubicBezierCurve D2)
    {
        Point2D[] curveC1 = C1.getCubicBezierCurve();
        Point2D[] curveC2 = C2.getCubicBezierCurve();
        Point2D[] curveD1 = D1.getCubicBezierCurve();
        Point2D[] curveD2 = D2.getCubicBezierCurve();
        
        int numberOfColorComponents = cornerColor[0].length;
        int szV = curveD1.length;
        int szU = curveC1.length;
        
        CoordinateColorPair[][] patchCC = new CoordinateColorPair[szV][szU];
        
        double stepV = (double) 1 / (szV - 1);
        double stepU = (double) 1 / (szU - 1);
        double v = - stepV;
        for(int i = 0; i < szV; i++)
        {
            v += stepV;
            double u = - stepU;
            for(int j = 0; j < szU; j++)
            {
                u += stepU;
                double scx = (1 - v) * curveC1[j].getX() + v * curveC2[j].getX();
                double scy = (1 - v) * curveC1[j].getY() + v * curveC2[j].getY();
                double sdx = (1 - u) * curveD1[i].getX() + u * curveD2[i].getX();
                double sdy = (1 - u) * curveD1[i].getY() + u * curveD2[i].getY();
                double sbx = (1 - v) * ((1 - u) * edgeC1[0].getX() + u * edgeC1[3].getX()) 
                        + v * ((1 - u) * edgeC2[0].getX() + u * edgeC2[3].getX());
                double sby = (1 - v) * ((1 - u) * edgeC1[0].getY() + u * edgeC1[3].getY()) 
                        + v * ((1 - u) * edgeC2[0].getY() + u * edgeC2[3].getY());
                
                double sx = scx + sdx - sbx;
                double sy = scy + sdy - sby;
                
                Point2D tmpC = new Point2D.Double(sx, sy);
                
                float[] paramSC = new float[numberOfColorComponents];
                for(int ci = 0; ci < numberOfColorComponents; ci++)
                {
                    paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
                            + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
                }
                patchCC[i][j] = new CoordinateColorPair(tmpC, paramSC);
            }
        }
        return patchCC;
    }
    
    private class CoordinateColorPair
    {
        final Point2D coordinate;
        final float[] color;
        
        CoordinateColorPair(Point2D p, float[] c)
        {
            coordinate = p;
            color = c.clone();
        }
    }
    
}
