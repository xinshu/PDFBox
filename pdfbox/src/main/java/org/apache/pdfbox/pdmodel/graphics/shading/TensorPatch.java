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
 *
 * @author Shaola
 */
public class TensorPatch
{
    protected final Point2D[][] tensorControlPoints;
    protected final float[][] cornerColor;
    private final int level = 4;
    
    protected final ArrayList<CoonsTriangle> listOfCoonsTriangle;
    
    public TensorPatch(Point2D[] tcp, float[][] color)
    {
        tensorControlPoints = reshapeControlPoints(tcp);
        cornerColor = color.clone();
        listOfCoonsTriangle = getCoonsTriangle();
    }
    
    private Point2D[][] reshapeControlPoints(Point2D[] tcp)
    {
        Point2D[][] square = new Point2D[4][4];
        for (int i = 0; i <= 3; i++)
        {
            square[0][i] = tcp[i];
            square[3][i] = tcp[9 - i];
        }
        for (int i = 1; i <= 2; i++)
        {
            square[i][0] = tcp[12 - i];
            square[i][2] = tcp[12 + i];
            square[i][3] = tcp[3 + i];
        }
        square[1][1] = tcp[12];
        square[2][1] = tcp[15];
        return square;
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle()
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor();
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
                
//                Point2D[] llCorner = {p0, p1, p3}; // counter clock wise
//                float[][] llColor = {patchCC[i-1][j-1].color, patchCC[i-1][j].color, patchCC[i][j-1].color};
//                CoonsTriangle tmpll = new CoonsTriangle(llCorner, llColor); // lower left triangle
//                list.add(tmpll);
//                Point2D[] urCorner = {p3, p1, p2}; // counter clock wise
//                float[][] urColor = {patchCC[i][j-1].color, patchCC[i-1][j].color, patchCC[i][j].color};
//                CoonsTriangle tmpur = new CoonsTriangle(urCorner, urColor); // upper right triangle
//                list.add(tmpur);
            }
        }
        return list;
    }
    
    private boolean overlaps(Point2D p0, Point2D p1)
    {
        return Math.abs(p0.getX() - p1.getX()) < 0.001 && Math.abs(p0.getY() - p1.getY()) < 0.001;
    }
    
    private CoordinateColorPair[][] getPatchCoordinatesColor()
    {
        int numberOfColorComponents = cornerColor[0].length;
        double[][] bernsteinPoly = getBernsteinPolynomials();
        int sz = bernsteinPoly[0].length;
        CoordinateColorPair[][] patchCC = new CoordinateColorPair[sz][sz];
        
        double step = (double) 1 / (sz - 1);
        double v = -step;
        for (int k = 0; k < sz; k++)
        {
            v += step;
            double u = - step;
            for (int l = 0; l < sz; l++)
            {
                double tmpx = 0.0;
                double tmpy = 0.0;
                for (int i = 0; i < 4; i++)
                {
                    for (int j = 0; j < 4; j++)
                    {
                        tmpx += tensorControlPoints[i][j].getX() * bernsteinPoly[i][l] * bernsteinPoly[j][k];
                        tmpy += tensorControlPoints[i][j].getY() * bernsteinPoly[i][l] * bernsteinPoly[j][k];
                    }
                }
                Point2D tmpC = new Point2D.Double(tmpx, tmpy);
                
                u += step;
                float[] paramSC = new float[numberOfColorComponents];
                for(int ci = 0; ci < numberOfColorComponents; ci++)
                {
                    paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
                            + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
                    //System.out.println("interpolated color: " + paramSC[ci]);
                }
                patchCC[k][l] = new CoordinateColorPair(tmpC, paramSC);
            }
        }
        return patchCC;
    }
    
    private double[][] getBernsteinPolynomials()
    {
        int sz = (1 << level) + 1;
        double[][] poly = new double[4][sz];
        double step = 1.0 / (sz - 1);
        double t = - step;
        for (int i = 0; i < sz; i++)
        {
            t += step;
            poly[0][i] = (1 - t) * (1 - t) * (1 - t);
            poly[1][i] = 3 * t * (1 - t) * (1 - t);
            poly[2][i] = 3 * t * t * (1 - t);
            poly[3][i] = t * t * t;
        }
        return poly;
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
