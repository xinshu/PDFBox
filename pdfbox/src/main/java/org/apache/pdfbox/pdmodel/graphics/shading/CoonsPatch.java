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
public class CoonsPatch
{
    protected final CubicBezierCurve edgeC1;
    protected final CubicBezierCurve edgeC2;
    protected final CubicBezierCurve edgeD1;
    protected final CubicBezierCurve edgeD2;
    protected final float[][] cornerColor;
    
    protected final ArrayList<CoonsTriangle> listOfCoonsTriangle;
    
    /**
     * Constructor for using 4 edges and color of 4 corners
     * @param C1 edge C1 of the coons patch, contains 4 control points [p1, p12, p11, p10] not [p10, p11, p12, p1]
     * @param C2 edge C2 of the coons patch, contains 4 control points [p4, p5, p6, p7]
     * @param D1 edge D1 of the coons patch, contains 4 control points [p1, p2, p3, p4]
     * @param D2 edge D2 of the coons patch, contains 4 control points [p10, p9, p8, p7] not [p7, p8, p9, p10]
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
        
        listOfCoonsTriangle = getCoonsTriangle();
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle()
    {
        return getCoonsTriangle(edgeC1, edgeC2, edgeD1, edgeD2, cornerColor);
    }
    
    private ArrayList<CoonsTriangle> getCoonsTriangle(CubicBezierCurve C1, CubicBezierCurve C2, CubicBezierCurve D1, CubicBezierCurve D2, 
                                float[][] color)
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        CoordinateColorPair[][] patchCC = getPatchCoordinatesColor(C1, C2, D1, D2); // at least 2 x 2, always square
        int sz = patchCC.length;
        for (int i = 1; i < sz; i++)
        {
            for (int j = 1; j < sz; j++)
            {
                Point2D[] llCorner = {patchCC[i-1][j-1].coordinate, patchCC[i-1][j].coordinate, patchCC[i][j-1].coordinate}; // counter clock wise
                float[][] llColor = {patchCC[i-1][j-1].color, patchCC[i-1][j].color, patchCC[i][j-1].color};
                CoonsTriangle tmpll = new CoonsTriangle(llCorner, llColor); // lower left triangle
                Point2D[] urCorner = {patchCC[i-1][j].coordinate, patchCC[i][j].coordinate, patchCC[i][j-1].coordinate}; // counter clock wise
                float[][] urColor = {patchCC[i-1][j].color, patchCC[i][j].color, patchCC[i][j-1].color};
                CoonsTriangle tmpur = new CoonsTriangle(urCorner, urColor); // upper right triangle
                list.add(tmpll);
                list.add(tmpur);
                //System.out.println("templl: " + tmpll.toString());
                //System.out.println("tempur: " + tmpur.toString());
            }
        }
        return list;
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
                double sbx = (1 - v) * ((1 - u) * edgeC1.controlPoints[0].getX() 
                        + u * edgeC1.controlPoints[3].getX()) 
                        + v * ((1 - u) * edgeC2.controlPoints[0].getX() 
                        + u * edgeC2.controlPoints[3].getX());
                double sby = (1 - v) * ((1 - u) * edgeC1.controlPoints[0].getY() 
                        + u * edgeC1.controlPoints[3].getY()) 
                        + v * ((1 - u) * edgeC2.controlPoints[0].getY() 
                        + u * edgeC2.controlPoints[3].getY());
                
                double sx = scx + sdx - sbx;
                double sy = scy + sdy - sby;
                
                Point2D tmpC = new Point2D.Double(sx, sy);
                //System.out.println("interpolated coordinates: " + tmpC);
                
                float[] paramSC = new float[numberOfColorComponents];
                for(int ci = 0; ci < numberOfColorComponents; ci++)
                {
                    paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
                            + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
                    //System.out.println("interpolated color: " + paramSC[ci]);
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
