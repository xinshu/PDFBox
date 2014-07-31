/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.common.PDRange;
import org.apache.pdfbox.util.Matrix;

/**
 * AWT PaintContext for Gouraud Triangle Lattice (Type 5) shading.
 * @author Tilman Hausherr
 * @author Shaola Ren
 */
class Type5ShadingContext extends GouraudShadingContext
{
    private static final Log LOG = LogFactory.getLog(Type5ShadingContext.class);

    /**
     * Constructor creates an instance to be used for fill operations.
     * @param shading the shading type to be used
     * @param cm the color model to be used
     * @param xform transformation for user to device space
     * @param ctm current transformation matrix
     * @param pageHeight height of the current page
     * @throws IOException if something went wrong
     */
    public Type5ShadingContext(PDShadingType5 shading, ColorModel cm, AffineTransform xform,
                               Matrix ctm, int pageHeight, Rectangle dBounds) throws IOException
    {
        super(shading, cm, xform, ctm, pageHeight, dBounds);

        LOG.debug("Type5ShadingContext");

        bitsPerColorComponent = shading.getBitsPerComponent();
        LOG.debug("bitsPerColorComponent: " + bitsPerColorComponent);
        bitsPerCoordinate = shading.getBitsPerCoordinate();
        LOG.debug(Math.pow(2, bitsPerCoordinate) - 1);
        triangleList = getTriangleList(xform,ctm);
        //System.out.println("type 5 shading triangleList's size: " + triangleList.size());
        pixelTable = calcPixelTable();
    }
    
    private ArrayList<CoonsTriangle> getTriangleList(AffineTransform xform, Matrix ctm) throws IOException
    {
        ArrayList<CoonsTriangle> list = new ArrayList<CoonsTriangle>();
        PDShadingType5 LatticeTriangleShadingType = (PDShadingType5) gouraudShadingType;
        COSDictionary cosDictionary = LatticeTriangleShadingType.getCOSDictionary();
        PDRange rangeX = LatticeTriangleShadingType.getDecodeForParameter(0);
        PDRange rangeY = LatticeTriangleShadingType.getDecodeForParameter(1);
        int numPerRow = LatticeTriangleShadingType.getVerticesPerRow();
        PDRange[] colRange = new PDRange[numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; ++i)
        {
            colRange[i] = LatticeTriangleShadingType.getDecodeForParameter(2 + i);
        }
        ArrayList<Vertex> vlist = new ArrayList<Vertex>(); 
        long maxSrcCoord = (long) Math.pow(2, bitsPerCoordinate) - 1;
        long maxSrcColor = (long) Math.pow(2, bitsPerColorComponent) - 1;
        COSStream cosStream = (COSStream) cosDictionary;
        
        ImageInputStream mciis = new MemoryCacheImageInputStream(cosStream.getUnfilteredStream());
        while(true)
        {
            Vertex p;
            try
            {
                p = readVertex(mciis, maxSrcCoord, maxSrcColor,rangeX, rangeY, colRange, ctm, xform);
                vlist.add(p);
                //System.out.println(p);
            }
            catch(EOFException ex)
            {
                break;
            }
        }
        int sz = vlist.size(), rowNum = sz / numPerRow;
        Vertex[][] latticeArray = new Vertex[rowNum][numPerRow];
        if (rowNum < 2)
        {
            return triangleList;
        }
        for (int i = 0; i < rowNum; i++)
        {
            for (int j = 0; j < numPerRow; j++)
            {
                latticeArray[i][j] = vlist.get(i * numPerRow + j);
            }
        }
        
        for (int i = 0; i < rowNum - 1; i++)
        {
            for (int j = 0; j < numPerRow - 1; j++)
            {
                Point2D[] ps = new Point2D[]
                                {
                                    latticeArray[i][j].point, latticeArray[i][j + 1].point, latticeArray[i + 1][j].point
                                };
                float[][] cs = new float[][]
                                {
                                    latticeArray[i][j].color, latticeArray[i][j + 1].color, latticeArray[i + 1][j].color
                                };
                list.add(new CoonsTriangle(ps, cs));
                ps = new Point2D[]
                                {
                                    latticeArray[i][j + 1].point, latticeArray[i + 1][j].point, latticeArray[i + 1][j + 1].point
                                };
                cs = new float[][]
                                {
                                    latticeArray[i][j + 1].color, latticeArray[i + 1][j].color, latticeArray[i + 1][j + 1].color
                                };
                list.add(new CoonsTriangle(ps, cs));
            }
        }
        return list;
    }
    
    @Override
    public void dispose()
    {
        super.dispose();
    }
}
