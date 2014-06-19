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
    
    private final int level;
    
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
        
        level = getLevel();
        
        listOfCoonsTriangle = getCoonsTriangle(level);
    }
    
    private int getLevel()
    {
        return 4;
    }
    
    private double getDistance(Point2D ps, Point2D pe)
    {
        double x = pe.getX() - ps.getX();
        double y = pe.getY() - ps.getY();
        return Math.sqrt(x * x + y * y);
    }
    
    private Point2D getMid(Point2D ps, Point2D pe)
    {
        return new Point2D.Double((ps.getX() + pe.getX()) / 2, (ps.getY() + pe.getY()) / 2);
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle(int l)
    {
        CubicBezierCurve eC1 = new CubicBezierCurve(edgeC1, l);
        CubicBezierCurve eC2 = new CubicBezierCurve(edgeC2, l);
        CubicBezierCurve eD1 = new CubicBezierCurve(edgeD1, l);
        CubicBezierCurve eD2 = new CubicBezierCurve(edgeD2, l);
        return getCoonsTriangle(eC1, eC2, eD1, eD2);
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle(CubicBezierCurve C1, CubicBezierCurve C2, CubicBezierCurve D1, CubicBezierCurve D2)
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor(C1, C2, D1, D2); // at least 2 x 2, always square
        int sz = patchCC.length;
        for (int i = 1; i < sz; i++)
        {
            for (int j = 1; j < sz; j++)
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
        int sz = curveC1.length;
        
        CoordinateColorPair[][] patchCC = new CoordinateColorPair[sz][sz];
        
        double step = (double) 1 / (sz - 1);
        double v = - step;
        for(int i = 0; i < sz; i++)
        {
            v += step;
            double u = - step;
            for(int j = 0; j < sz; j++)
            {
                u += step;
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
