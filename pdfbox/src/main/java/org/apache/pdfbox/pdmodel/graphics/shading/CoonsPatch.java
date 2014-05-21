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
    
    private final int horizontalElementNum;
    private final int verticalElementNum;
    
    private final Point2D[] curveC1;
    private final Point2D[] curveC2;
    private final Point2D[] curveD1;
    private final Point2D[] curveD2; 
    
    /**
     * Constructor for using 4 edges and color of 4 corners
     * @param c1 edge C1 of the coons patch, contains 4 control points [p1, p12, p11, p10]
     * @param c2 edge C2 of the coons patch, contains 4 control points [p4, p5, p6, p7]
     * @param d1 edge D1 of the coons patch, contains 4 control points [p1, p2, p3, p4]
     * @param d2 edge D2 of the coons patch, contains 4 control points [p10, p9, p8, p7]
     * @param color 4 corner colors, value is as [c1, c2, c3, c4]
     */
    public CoonsPatch(CubicBezierCurve c1, CubicBezierCurve c2, CubicBezierCurve d1, CubicBezierCurve d2, 
            float[][] color)
    {
        edgeC1 = c1;
        edgeC2 = c2;
        edgeD1 = d1;
        edgeD2 = d2;
        cornerColor = color.clone();
        
        int h1 = (int) Math.abs(edgeC1.controlPoints[0].getX() - edgeC1.controlPoints[3].getX());
        int h2 = (int) Math.abs(edgeC2.controlPoints[0].getX() - edgeC2.controlPoints[3].getX());
        int v1 = (int) Math.abs(edgeD1.controlPoints[0].getY() - edgeD1.controlPoints[3].getY());
        int v2 = (int) Math.abs(edgeD2.controlPoints[0].getY() - edgeD2.controlPoints[3].getY());
        
        horizontalElementNum = Math.max(h1, h2) + 1;
        verticalElementNum = Math.max(v1, v2) + 1;
        
        curveC1 = edgeC1.getCubicBezierCurve(horizontalElementNum);
        curveC2 = edgeC2.getCubicBezierCurve(horizontalElementNum);
        curveD1 = edgeD1.getCubicBezierCurve(verticalElementNum);
        curveD2 = edgeD2.getCubicBezierCurve(verticalElementNum);
    }
    
    public float[] getParamSpaceColor(int numberOfColorComponents, int i, int j)
    {
        float[] paramSC = new float[numberOfColorComponents];
        
        double stepu = 1 / (horizontalElementNum - 1);
        double stepv = 1 / (verticalElementNum - 1);
        
        double u = stepu * j;
        double v = stepv * i;
        for(int ci = 0; ci < numberOfColorComponents; ci++)
        {
            paramSC[ci] = (float) ((1 - v) * ((1 - u) * cornerColor[0][ci] + u * cornerColor[3][ci]) 
                    + v * ((1 - u) * cornerColor[1][ci] + u * cornerColor[2][ci]));
        }
        return paramSC;
    }
    
    public Point[][] getPatchCoordinates()
    {
        Point[][] patchCo = new Point[verticalElementNum][horizontalElementNum];
        
        double stepu = 1 / (horizontalElementNum - 1);
        double stepv = 1 / (verticalElementNum - 1);
        
        double v = -stepv;
        for(int i = 0; i < verticalElementNum; i++)
        {
            v += stepv;
            double u = -stepu;
            for(int j = 0; j < horizontalElementNum; j++)
            {
                u += stepu;
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
                
                int sx = (int) Math.round(scx + sdx - sbx);
                int sy = (int) Math.round(scy + sdy - sby);
                
                patchCo[i][j] = new Point(sx, sy);
            }
        }
        return patchCo;
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
