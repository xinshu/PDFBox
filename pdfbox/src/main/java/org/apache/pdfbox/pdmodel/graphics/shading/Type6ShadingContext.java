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

import java.awt.PaintContext;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.common.PDRange;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.util.Matrix;

/**
 *
 * @author Shaola
 */
public class Type6ShadingContext implements PaintContext
{
    private static final Log LOG = LogFactory.getLog(Type6ShadingContext.class);
    
    private ColorModel outputColorModel;
    private PDColorSpace shadingColorSpace;

    /** number of color components. */
    private final int numberOfColorComponents;

    /** patch list. */
    private ArrayList<CoonsPatch> patchList;

    /** bits per coordinate. */
    private final int bitsPerCoordinate;

    /** bits per color component. */
    private final int bitsPerColorComponent;

    /** bits per flag. */
    private final int bitsPerFlag;
    
    /** background values.*/
    private float[] background;

    private final boolean hasFunction;
    private final PDShading coonsShadingType;
    
     /**
     * Constructor creates an instance to be used for fill operations.
     * @param shading the shading type to be used
     * @param colorModel the color model to be used
     * @param xform transformation for user to device space
     * @param ctm current transformation matrix
     * @param pageHeight height of the current page
     * @throws IOException if something went wrong
     */
    public Type6ShadingContext(PDShadingType6 shading, ColorModel colorModel, AffineTransform xform,
                                Matrix ctm, int pageHeight) throws IOException
    {
        coonsShadingType = shading;
        patchList = new ArrayList<CoonsPatch>();
        hasFunction = shading.getFunction() != null;
        //System.out.println("has Function: " + hasFunction);
        shadingColorSpace = shading.getColorSpace();
        
        numberOfColorComponents = hasFunction ? 1 : shadingColorSpace.getNumberOfComponents();
        //System.out.println("numberOfColorComponents: " + numberOfColorComponents);
        
        bitsPerColorComponent = shading.getBitsPerComponent();
        
        bitsPerCoordinate = shading.getBitsPerCoordinate();
        
        bitsPerFlag = shading.getBitsPerFlag();
        
        long maxSrcCoord = (int) Math.pow(2, bitsPerCoordinate) - 1;
        long maxSrcColor = (int) Math.pow(2, bitsPerColorComponent) - 1;

        // create the output color model using RGB+alpha as color space
        ColorSpace outputCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        outputColorModel = new ComponentColorModel(outputCS, true, false, Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
        
        COSArray bg = shading.getBackground();
        if (bg != null)
        {
            background = bg.toFloatArray();
        }
        
        COSDictionary cosDictionary = shading.getCOSDictionary();
        COSStream cosStream = (COSStream) cosDictionary;
        
        COSArray decode = (COSArray) cosDictionary.getDictionaryObject(COSName.DECODE);
        PDRange rangeX = shading.getDecodeForParameter(0);
        PDRange rangeY = shading.getDecodeForParameter(1);

        PDRange[] colRange = new PDRange[numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; ++i)
        {
            colRange[i] = shading.getDecodeForParameter(2 + i);
        }
        
        ImageInputStream mciis = new MemoryCacheImageInputStream(cosStream.getFilteredStream());
        
        CubicBezierCurve implicitEdge = null;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        
        byte flag = (byte) 0;
        
        try
        {
            flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
        }
        catch (EOFException ex)
        {
        }
        
        while (true)
        {
            try
            {
                boolean isFree = (flag == 0);
                CoonsPatch current = readCoonsPatch(mciis, isFree, implicitEdge, implicitCornerColor,
                        maxSrcCoord, maxSrcColor, rangeX, rangeY, colRange, ctm, xform);
                if (current == null)
                {
                    break;
                }
                patchList.add(current);
                flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
                //System.out.println("flag: " + flag);
                switch (flag)
                {
                    case 0:
                        break;
                    case 1:
                        implicitEdge = current.edgeC2;
                        for (int i = 0; i < numberOfColorComponents; i++)
                        {
                            implicitCornerColor[0][i] = current.cornerColor[1][i];
                            implicitCornerColor[1][i] = current.cornerColor[2][i];
                        }
                        break;
                    case 2:
                        Point2D[] implicitControlPointsFlag2 = {current.edgeD2.controlPoints[3], current.edgeD2.controlPoints[2], 
                                current.edgeD2.controlPoints[1], current.edgeD2.controlPoints[0]};
                        implicitEdge = new CubicBezierCurve(implicitControlPointsFlag2);
                        for (int i = 0; i < numberOfColorComponents; i++)
                        {
                            implicitCornerColor[0][i] = current.cornerColor[2][i];
                            implicitCornerColor[1][i] = current.cornerColor[3][i];
                        }
                        break;
                    case 3:
                        Point2D[] implicitControlPointsFlag3 = {current.edgeC1.controlPoints[3], current.edgeC1.controlPoints[2], 
                                current.edgeC1.controlPoints[1], current.edgeC1.controlPoints[0]};
                        implicitEdge = new CubicBezierCurve(implicitControlPointsFlag3);
                        for (int i = 0; i < numberOfColorComponents; i++)
                        {
                            implicitCornerColor[0][i] = current.cornerColor[3][i];
                            implicitCornerColor[1][i] = current.cornerColor[0][i];
                        }
                        break;
                    default:
                        LOG.warn("bad flag: " + flag);
                        break;
                } 
            }
            catch (EOFException ex)
            {
                break;
            }
        }
        mciis.close();
    }
    
    private void transformPoint(Point2D p, Matrix ctm, AffineTransform xform)
    {
        if (ctm != null)
        {
            ctm.createAffineTransform().transform(p, p);
            xform.transform(p, p);
        }
    }
    
    private CoonsPatch readCoonsPatch(ImageInputStream input, boolean isFree, CubicBezierCurve implicitEdge, 
                                float[][] implicitCornerColor, long maxSrcCoord, long maxSrcColor, 
                                PDRange rangeX, PDRange rangeY, PDRange[] colRange, 
                                Matrix ctm, AffineTransform xform) throws IOException
    {
        float[][] color = new float[4][numberOfColorComponents];
        
        Point2D[] points = new Point2D[12];
        
        int pStart = 4, cStart = 2;
        if (isFree)
        {
            pStart = 0;
            cStart = 0;
        }
        else
        {
            points[0] = implicitEdge.controlPoints[0];
            points[1] = implicitEdge.controlPoints[1];
            points[2] = implicitEdge.controlPoints[2];
            points[3] = implicitEdge.controlPoints[3];
            
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                color[0][i] = implicitCornerColor[0][i];
                color[1][i] = implicitCornerColor[1][i];
            }
        }
        
        try
        {
            for (int i = pStart; i < 12; i++)
            {
                long x = input.readBits(bitsPerCoordinate);
                long y = input.readBits(bitsPerCoordinate);
                double px = interpolate(x, maxSrcCoord, rangeX.getMin(), rangeX.getMax());
                double py = interpolate(y, maxSrcCoord, rangeY.getMin(), rangeY.getMax());
                Point2D tmp = new Point2D.Double(px, py);
                transformPoint(tmp, ctm, xform);
                points[i] = tmp;
                //System.out.println("x: " + x + " " + maxSrcCoord + " " + rangeX.getMin() + " " + rangeX.getMax());
                //System.out.println("y: " + y + " " + maxSrcCoord + " " + rangeY.getMin() + " " + rangeY.getMax());
                //System.out.println("interpolate: " + tmp.getX() + " " + tmp.getY());
            }
            for (int i = cStart; i < 4; i++)
            {
                for (int j = 0; j < numberOfColorComponents; j++)
                {
                    int c = (int) input.readBits(bitsPerColorComponent);
                    color[i][j] = (float) interpolate(c, maxSrcColor, colRange[j].getMin(), colRange[j].getMax());
                    //System.out.println("color: " + j + " " + c + " " + color[i][j]);
                }
            }
            
//            if (!hasFunction){
//                for (int i = cStart; i < 4; i++)
//                {
//                    for (int j = 0; j < numberOfColorComponents; j++)
//                    {
//                        int c = (int) input.readBits(bitsPerColorComponent);
//                        color[i][j] = (float) interpolate(c, maxSrcColor, colRange[j].getMin(), colRange[j].getMax());
//                        //System.out.println("color: " + j + " " + c + " " + color[i][j]);
//                    }
//                }
//            }
//            // need to edit to real value
//            else
//            {
//                for (int i = cStart; i < 4; i++)
//                {
//                    for (int j = 0; j < numberOfColorComponents; j++)
//                    {
//                        color[i][j] = 0.5f;
//                    }
//                    int c = (int) input.readBits(bitsPerColorComponent);
//                    //System.out.println("color: " + 0 + " " + c);
//                }
//            }
        }
        catch(EOFException ex)
        {
            LOG.debug("EOF");
            return null;
        }
        
        CubicBezierCurve d1 = new CubicBezierCurve(new Point2D[]
                            {
                                points[0], points[1], points[2], points[3]
                            });
        CubicBezierCurve c2 = new CubicBezierCurve(new Point2D[]
                            {
                                points[3], points[4], points[5], points[6]
                            });
        CubicBezierCurve d2 = new CubicBezierCurve(new Point2D[]
                            {
                                //points[6], points[7], points[8], points[9]
                                points[9], points[8], points[7], points[6]
                            });
        CubicBezierCurve c1 = new CubicBezierCurve(new Point2D[]
                            {
                                //points[9], points[10], points[11], points[0]
                                points[0], points[11], points[10], points[9]
                            });
        
        return new CoonsPatch(c1, c2, d1, d2, color);
    }
    
    private double interpolate(float x, long maxValue, float rangeMin, float rangeMax)
    {
        return rangeMin + (double)(x / maxValue) * (rangeMax - rangeMin); 
    }
    
    @Override
    public void dispose()
    {
        patchList = null;
        outputColorModel = null;
        shadingColorSpace = null;
    }
    
    @Override
    public final ColorModel getColorModel()
    {
        return outputColorModel;
    }
    
    @Override
    public final Raster getRaster(int x, int y, int w, int h)
    {
        // to do, need to edit the concrete content
        WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
        int[] data = new int[w * h * 4];
        
        if (!patchList.isEmpty() || background != null)
        {
            //System.out.println("patchList.size(): " + patchList.size());
            for (int row = 0; row < h; row++)
            {
                for (int col = 0; col < w; col++)
                {
                    Point2D p = new Point(x + col, y + row);
                    float[] values = null;
                    for (CoonsPatch it : patchList)
                    {
                        for (CoonsTriangle tri : it.listOfCoonsTriangle)
                        {
                            if (tri.contains(p))
                            {
                                values = tri.getColor(p);
                            }
                        }
                    }
                    
//                    CoonsPatch it = patchList.get(3);
//                    for (CoonsTriangle tri : it.listOfCoonsTriangle)
//                    {
//                        if (tri.contains(p))
//                        {
//                            values = tri.getColor(p);
//                        }
//                    }
                    if (values == null)
                    {
                        if (background != null)
                        {
                            values = background;
                        }
                        else
                        {
                            continue;
                        }
                    }
                    
                    if (hasFunction)
                    {
                        try
                        {
                            values = coonsShadingType.evalFunction(values);
                        }
                        catch (IOException exception)
                        {
                            LOG.error("error while processing a function", exception);
                        }
                    }
                    
                    try
                    {
                        values = shadingColorSpace.toRGB(values);
                    }
                    catch (IOException exception)
                    {
                        LOG.error("error processing color space", exception);
                    }

                    int index = (row * w + col) * 4;
                    data[index] = (int) (values[0] * 255);
                    data[index + 1] = (int) (values[1] * 255);
                    data[index + 2] = (int) (values[2] * 255);
                    data[index + 3] = 255;
                }
            }
        }
        raster.setPixels(0, 0, w, h, data);
        return raster;
    }
}
