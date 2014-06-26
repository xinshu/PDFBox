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
 * @author Shaola Ren
 */
class TensorPatch
{
    protected final Point2D[][] tensorControlPoints;
    protected final float[][] cornerColor;
    private final int[] level; // {levelU, levelV}
    
    protected final ArrayList<CoonsTriangle> listOfCoonsTriangle;
    
    public TensorPatch(Point2D[] tcp, float[][] color)
    {
        tensorControlPoints = reshapeControlPoints(tcp);
        cornerColor = color.clone();
        level = setLevel();
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
    
    private int[] setLevel()
    {
        int[] l = {4, 4};
        
        Point2D[] ctlC1 = new Point2D[4];
        Point2D[] ctlC2 = new Point2D[4];
        for (int j = 0; j < 4; j++)
        {
            ctlC1[j] = tensorControlPoints[j][0];
            ctlC2[j] = tensorControlPoints[j][3];
        }
        if (isEdgeALine(ctlC1) & isEdgeALine(ctlC2))
        {
            if (isOnSameSideCC(tensorControlPoints[1][1]) | isOnSameSideCC(tensorControlPoints[1][2]) |
                                isOnSameSideCC(tensorControlPoints[2][1]) | isOnSameSideCC(tensorControlPoints[2][2]))
            {
            }
            else
            {
                double lc1 = getLen(ctlC1[0], ctlC1[3]), lc2 = getLen(ctlC2[0], ctlC2[3]);
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
        }
        
        if (isEdgeALine(tensorControlPoints[0]) & isEdgeALine(tensorControlPoints[3]))
        {
            if (isOnSameSideDD(tensorControlPoints[1][1]) | isOnSameSideDD(tensorControlPoints[1][2]) |
                                isOnSameSideDD(tensorControlPoints[2][1]) | isOnSameSideDD(tensorControlPoints[2][2]))
            {
            }
            else
            {
                double ld1 = getLen(tensorControlPoints[0][0], tensorControlPoints[0][3]);
                double ld2 = getLen(tensorControlPoints[3][0], tensorControlPoints[3][3]);
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
        }
        return l;
    }
    
    private double getLen(Point2D ps, Point2D pe)
    {
        double x = pe.getX() - ps.getX();
        double y = pe.getY() - ps.getY();
        return Math.sqrt(x * x + y * y);
    }
    
    private boolean isOnSameSideCC(Point2D p)
    {
        double cc = edgeEquationValue(p, tensorControlPoints[0][0], tensorControlPoints[3][0]) * 
                                edgeEquationValue(p, tensorControlPoints[0][3], tensorControlPoints[3][3]);
        return cc > 0;
    }
    
    private boolean isOnSameSideDD(Point2D p)
    {
        double dd = edgeEquationValue(p, tensorControlPoints[0][0], tensorControlPoints[0][3]) * 
                                edgeEquationValue(p, tensorControlPoints[3][0], tensorControlPoints[3][3]);
        return dd > 0;
    }
    
    private boolean isEdgeALine(Point2D[] ctl)
    {
//        HashSet<Integer> setX = new HashSet<Integer>();
//        HashSet<Integer> setY = new HashSet<Integer>();
//        for (Point2D p : ctl)
//        {
//            setX.add((int)(p.getX() * 1000));
//            setY.add((int)(p.getY() * 1000));
//        }
//        return setX.size() == 1 || setY.size() == 1;
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
    
    private ArrayList<CoonsTriangle> getCoonsTriangle()
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor();
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
        return Math.abs(p0.getX() - p1.getX()) < 0.0001 && Math.abs(p0.getY() - p1.getY()) < 0.001;
    }
    
    private CoordinateColorPair[][] getPatchCoordinatesColor()
    {
        int numberOfColorComponents = cornerColor[0].length;
        double[][] bernsteinPolyU = getBernsteinPolynomials(level[0]);
        int szU = bernsteinPolyU[0].length;
        double[][] bernsteinPolyV = getBernsteinPolynomials(level[1]);
        int szV = bernsteinPolyV[0].length;
        CoordinateColorPair[][] patchCC = new CoordinateColorPair[szV][szU];
        
        //double stepU = (double) 1 / (szU - 1);
        //double stepV = (double) 1 / (szV - 1);
        double stepU = 1.0 / (szU - 1);
        double stepV = 1.0 / (szV - 1);
        double v = -stepV;
        for (int k = 0; k < szV; k++)
        {
            v += stepV;
            double u = - stepU;
            for (int l = 0; l < szU; l++)
            {
                double tmpx = 0.0;
                double tmpy = 0.0;
                for (int i = 0; i < 4; i++)
                {
                    for (int j = 0; j < 4; j++)
                    {
                        tmpx += tensorControlPoints[i][j].getX() * bernsteinPolyU[i][l] * bernsteinPolyV[j][k];
                        tmpy += tensorControlPoints[i][j].getY() * bernsteinPolyU[i][l] * bernsteinPolyV[j][k];
                    }
                }
                Point2D tmpC = new Point2D.Double(tmpx, tmpy);
                
                u += stepU;
                float[] paramSC = new float[numberOfColorComponents];
                for(int ci = 0; ci < numberOfColorComponents; ci++)
                {
                    paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
                            + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
                }
                patchCC[k][l] = new CoordinateColorPair(tmpC, paramSC);
            }
        }
        return patchCC;
    }
    
    private double[][] getBernsteinPolynomials(int lvl)
    {
        int sz = (1 << lvl) + 1;
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
