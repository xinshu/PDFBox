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
import java.awt.Rectangle;
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
import java.util.HashMap;
import java.util.HashSet;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.common.PDRange;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.util.Matrix;

/**
 * This class is extended in Type6ShadingContext and Type7ShadingContext.
 * This was done as part of GSoC2014, Tilman Hausherr is the mentor.
 * @author Shaola Ren
 */
abstract class PatchMeshesShadingContext implements PaintContext
{
    private static final Log LOG = LogFactory.getLog(PatchMeshesShadingContext.class);
    
    protected ColorModel outputColorModel;
    protected PDColorSpace shadingColorSpace;
    private final Rectangle deviceBounds;
    protected final int numberOfColorComponents; // number of color components
    protected float[] background; // background values.
    protected int rgbBackground;
    protected final boolean hasFunction;
    protected final PDShading patchMeshesShadingType;
    private PDRectangle bboxRect;
    private float[] bboxTab = new float[4];
    
    // the following fields are not intialized in this abstract class
    protected ArrayList<Patch> patchList; // patch list
    protected int bitsPerCoordinate; // bits per coordinate
    protected int bitsPerColorComponent; // bits per color component
    protected int bitsPerFlag; // bits per flag
    protected HashMap<Point, Integer> pixelTable;
    
    /**
     * Constructor creates an instance to be used for fill operations.
     * @param shading the shading type to be used
     * @param colorModel the color model to be used
     * @param xform transformation for user to device space
     * @param ctm current transformation matrix
     * @param pageHeight height of the current page
     * @param dBounds device bounds
     * @throws IOException if something went wrong
     */
    protected PatchMeshesShadingContext(PDShading shading, ColorModel colorModel, AffineTransform xform,
                                Matrix ctm, int pageHeight, Rectangle dBounds) throws IOException
    {
        patchMeshesShadingType = shading;
        deviceBounds = dBounds;
        bboxRect = shading.getBBox();
        if (bboxRect != null)
        {
            bboxTab[0] = bboxRect.getLowerLeftX();
            bboxTab[1] = bboxRect.getLowerLeftY();
            bboxTab[2] = bboxRect.getUpperRightX();
            bboxTab[3] = bboxRect.getUpperRightY();
            if (ctm != null)
            {
                // transform the coords using the given matrix
                ctm.createAffineTransform().transform(bboxTab, 0, bboxTab, 0, 2);
            }
             xform.transform(bboxTab, 0, bboxTab, 0, 2);
        }
        reOrder(bboxTab, 0, 2);
        reOrder(bboxTab, 1, 3);
        if (bboxTab[0] >= bboxTab[2] || bboxTab[1] >= bboxTab[3])
        {
            bboxRect = null;
        }
        
        patchList = new ArrayList<Patch>();
        hasFunction = shading.getFunction() != null;
        shadingColorSpace = shading.getColorSpace();
        
        numberOfColorComponents = hasFunction ? 1 : shadingColorSpace.getNumberOfComponents();
        
        // create the output color model using RGB+alpha as color space
        ColorSpace outputCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        outputColorModel = new ComponentColorModel(outputCS, true, false, Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
        
        COSArray bg = shading.getBackground();
        if (bg != null)
        {
            background = bg.toFloatArray();
            rgbBackground = convertToRGB(background);
        }
    }
    
    // this method is used to arrange the array to denote the left upper corner and right lower corner of the BBox
    private void reOrder(float[] array, int i, int j)
    {
        if (i < j && array[i] <= array[j])
        {
        }
        else
        {
            float tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    // transform a point from source space to device space
    private void transformPoint(Point2D p, Matrix ctm, AffineTransform xform)
    {
        if (ctm != null)
        {
            ctm.createAffineTransform().transform(p, p);
        }
        xform.transform(p, p);
    }
    
    /**
     * Create a patch list from a data stream, the returned list contains all the patches 
     * contained in the data stream.
     * @param xform transformation for user to device space
     * @param ctm current transformation matrix
     * @param cosDictionary dictionary object to give the image information
     * @param rangeX range for coordinate x
     * @param rangeY range for coordinate y
     * @param colRange range for color
     * @param numP number of control points, 12 for type 6 shading and 16 for type 7 shading
     * @return the obtained patch list
     * @throws IOException when something went wrong
     */
    protected ArrayList<Patch> getPatchList(AffineTransform xform,Matrix ctm, COSDictionary cosDictionary, 
                                PDRange rangeX, PDRange rangeY, PDRange[] colRange, int numP) throws IOException
    {
        ArrayList<Patch> list = new ArrayList<Patch>();
        long maxSrcCoord = (long) Math.pow(2, bitsPerCoordinate) - 1;
        long maxSrcColor = (long) Math.pow(2, bitsPerColorComponent) - 1;
        COSStream cosStream = (COSStream) cosDictionary;
        
        ImageInputStream mciis = new MemoryCacheImageInputStream(cosStream.getUnfilteredStream());
        
        Point2D[] implicitEdge = new Point2D[4];
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        
        byte flag = (byte) 0;
        
        try
        {
            flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
        }
        catch (EOFException ex)
        {
            LOG.error(ex);
        }
        
        while (true)
        {
            try
            {
                boolean isFree = (flag == 0);
                Patch current = readPatch(mciis, isFree, implicitEdge, implicitCornerColor,
                                maxSrcCoord, maxSrcColor, rangeX, rangeY, colRange, ctm, xform, numP);
                if (current == null)
                {
                    break;
                }
                list.add((Patch) current);
                flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
                switch (flag)
                {
                    case 0:
                        break;
                    case 1:
                        implicitEdge = current.getFlag1Edge();
                        implicitCornerColor = current.getFlag1Color();
                        break;
                    case 2:
                        implicitEdge = current.getFlag2Edge();
                        implicitCornerColor = current.getFlag2Color();
                        break;
                    case 3:
                        implicitEdge = current.getFlag3Edge();
                        implicitCornerColor = current.getFlag3Color();
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
        return list;
    }
    
    /**
     * Read a single patch from a data stream, a patch contains information of 
     * its coordinates and color parameters.
     * @param input the image source data stream
     * @param isFree whether this is a free patch
     * @param implicitEdge implicit edge when a patch is not free, otherwise it's not used
     * @param implicitCornerColor implicit colors when a patch is not free, otherwise it's not used
     * @param maxSrcCoord the maximum coordinate value calculated from source data
     * @param maxSrcColor the maximum color value calculated from source data
     * @param rangeX range for coordinate x
     * @param rangeY range for coordinate y
     * @param colRange range for color
     * @param ctm current transformation matrix
     * @param xform transformation for user to device space
     * @param numP number of control points, 12 for type 6 shading and 16 for type 7 shading
     * @return a single patch
     * @throws IOException when something went wrong
     */
    protected Patch readPatch(ImageInputStream input, boolean isFree, Point2D[] implicitEdge, 
                                float[][] implicitCornerColor, long maxSrcCoord, long maxSrcColor, 
                                PDRange rangeX, PDRange rangeY, PDRange[] colRange, 
                                Matrix ctm, AffineTransform xform, int numP) throws IOException
    {
        float[][] color = new float[4][numberOfColorComponents];
        Point2D[] points = new Point2D[numP];
        int pStart = 4, cStart = 2;
        if (isFree)
        {
            pStart = 0;
            cStart = 0;
        }
        else
        {
            points[0] = implicitEdge[0];
            points[1] = implicitEdge[1];
            points[2] = implicitEdge[2];
            points[3] = implicitEdge[3];
            
            for (int i = 0; i < numberOfColorComponents; i++)
            {
                color[0][i] = implicitCornerColor[0][i];
                color[1][i] = implicitCornerColor[1][i];
            }
        }
        
        try
        {
            for (int i = pStart; i < numP; i++)
            {
                long x = input.readBits(bitsPerCoordinate);
                long y = input.readBits(bitsPerCoordinate);
                double px = interpolate(x, maxSrcCoord, rangeX.getMin(), rangeX.getMax());
                double py = interpolate(y, maxSrcCoord, rangeY.getMin(), rangeY.getMax());
                Point2D tmp = new Point2D.Double(px, py);
                transformPoint(tmp, ctm, xform);
                points[i] = tmp;
            }
            for (int i = cStart; i < 4; i++)
            {
                for (int j = 0; j < numberOfColorComponents; j++)
                {
                    long c = input.readBits(bitsPerColorComponent);
                    color[i][j] = (float) interpolate(c, maxSrcColor, colRange[j].getMin(), colRange[j].getMax());
                }
            }
        }
        catch(EOFException ex)
        {
            LOG.debug("EOF");
            return null;
        }
        return generatePatch(points, color);
    }

    /**
     * Create a patch using control points and 4 corner color values, in Type6ShadingContext, 
     * a CoonsPatch is returned; in Type6ShadingContext, a TensorPatch is returned.
     * @param points 12 or 16 control points
     * @param color 4 corner colors
     * @return a patch instance
     */
    abstract Patch generatePatch(Point2D[] points, float[][] color);
    
    // get a point coordinate on a line by linear interpolation
    private double interpolate(double x, long maxValue, float rangeMin, float rangeMax)
    {
        return rangeMin + (x / maxValue) * (rangeMax - rangeMin);
    }
    
    /**
     * Calculate every point and its color and store them in a Hash table.
     * @return a Hash table which contains all the points' positions and colors of one image
     */
    protected HashMap<Point, Integer> calcPixelTable()
    {
        HashMap<Point, Integer> map = new HashMap<Point, Integer>();
        for (Patch it : patchList)
        {
            for (CoonsTriangle tri : it.listOfCoonsTriangle)
            {
                int degree = tri.getDeg();
                if (degree == 2)
                {
                    Line line = tri.getLine();
                    HashSet<Point> linePoints = line.linePoints;
                    for (Point p : linePoints)
                    {
                        float[] values = line.getColor(p);
                        map.put(p, convertToRGB(values));
                    }
                }
                else
                {
                    int[] boundary = tri.getBoundary();
                    boundary[0] = Math.max(boundary[0], deviceBounds.x);
                    boundary[1] = Math.min(boundary[1], deviceBounds.x + deviceBounds.width);
                    boundary[2] = Math.max(boundary[2], deviceBounds.y);
                    boundary[3] = Math.min(boundary[3], deviceBounds.y + deviceBounds.height);
                    for (int x = boundary[0]; x <= boundary[1]; x++)
                    {
                        for (int y = boundary[2]; y <= boundary[3]; y++)
                        {
                            Point p = new Point(x, y);
                            if (tri.contains(p))
                            {
                                float[] values = tri.getColor(p);
                                map.put(p, convertToRGB(values));
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
    
    // convert color to RGB color values
    private int convertToRGB(float[] values)
    {
        float[] nValues = null;
        float[] rgbValues = null;
        int normRGBValues = 0;
        if (hasFunction)
        {
            try
            {
                nValues = patchMeshesShadingType.evalFunction(values);
            }
            catch (IOException exception)
            {
                LOG.error("error while processing a function", exception);
            }
        }

        try
        {
            if (nValues == null)
            {
                rgbValues = shadingColorSpace.toRGB(values);
            }
            else
            {
                rgbValues = shadingColorSpace.toRGB(nValues);
            }
            normRGBValues = (int) (rgbValues[0] * 255);
            normRGBValues |= (((int) (rgbValues[1] * 255)) << 8);
            normRGBValues |= (((int) (rgbValues[2] * 255)) << 16);
        }
        catch (IOException exception)
        {
            LOG.error("error processing color space", exception);
        }
        return normRGBValues;
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
        WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
        int[] data = new int[w * h * 4];       
        if (!patchList.isEmpty() || background != null)
        {
            for (int row = 0; row < h; row++)
            {
                int currentY = y + row;
                if (bboxRect != null)
                {
                    if (currentY < bboxTab[1] || currentY > bboxTab[3])
                    {
                        continue;
                    }
                }
                for (int col = 0; col < w; col++)
                {
                    int currentX = x + col;
                    if (bboxRect != null)
                    {
                        if (currentX < bboxTab[0] || currentX > bboxTab[2])
                        {
                            continue;
                        }
                    }
                    Point p = new Point(x + col, y + row);
                    int value;
                    if (pixelTable.containsKey(p))
                    {
                        value = pixelTable.get(p);
                    }
                    else
                    {
                        if (background != null)
                        {
                            value = rgbBackground;
                        }
                        else
                        {
                            continue;
                        }
                    }
                    int index = (row * w + col) * 4;
                    data[index] = value & 255;
                    value >>= 8;
                    data[index + 1] = value & 255;
                    value >>= 8;
                    data[index + 2] = value & 255;
                    data[index + 3] = 255;
                }
            }
        }
        raster.setPixels(0, 0, w, h, data);
        return raster;
    }
}
