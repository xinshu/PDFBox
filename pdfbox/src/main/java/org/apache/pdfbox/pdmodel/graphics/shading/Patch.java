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
abstract class Patch
{
    protected Point2D[][] controlPoints;
    protected float[][] cornerColor; 
    protected int[] level; // {levelU, levelV}
    protected ArrayList<CoonsTriangle> listOfCoonsTriangle;
    
    public Patch(Point2D[] ctl, float[][] color)
    {
        cornerColor = color.clone();
    }
    
    protected abstract Point2D[] getFlag1Edge();
    protected abstract Point2D[] getFlag2Edge();
    protected abstract Point2D[] getFlag3Edge();
    
    protected float[][] getFlag1Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[1][i];
            implicitCornerColor[1][i] = cornerColor[2][i];
        }
        return implicitCornerColor;
    }
    
    protected float[][] getFlag2Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[2][i];
            implicitCornerColor[1][i] = cornerColor[3][i];
        }
        return implicitCornerColor;
    }
    
    protected float[][] getFlag3Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[3][i];
            implicitCornerColor[1][i] = cornerColor[0][i];
        }
        return implicitCornerColor;
    }
    
    protected double getLen(Point2D ps, Point2D pe)
    {
        double x = pe.getX() - ps.getX();
        double y = pe.getY() - ps.getY();
        return Math.sqrt(x * x + y * y);
    }
    
    protected boolean isEdgeALine(Point2D[] ctl)
    {
        double ctl1 = Math.abs(edgeEquationValue(ctl[1], ctl[0], ctl[3]));
        double ctl2 = Math.abs(edgeEquationValue(ctl[2], ctl[0], ctl[3]));
        double x = Math.abs(ctl[0].getX() - ctl[3].getX());
        double y = Math.abs(ctl[0].getY() - ctl[3].getY());
        return (ctl1 <= x && ctl2 <= x) || (ctl1 <= y && ctl2 <= y);
    }
    
    protected double edgeEquationValue(Point2D p, Point2D p1, Point2D p2)
    {
        return (p2.getY() - p1.getY()) * (p.getX() - p1.getX()) - (p2.getX() - p1.getX()) * (p.getY() - p1.getY());
    }
    
    protected ArrayList<CoonsTriangle> getCoonsTriangle(CoordinateColorPair[][] patchCC)
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
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
}
