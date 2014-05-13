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

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRange;
import org.apache.pdfbox.util.Matrix;

import java.awt.Paint;

/**
 * Resources for a shading type 4 (Free-Form Gouraud-Shaded Triangle Mesh).
 */
public class PDShadingType4 extends PDShading
{
    // an array of 2^n numbers specifying the linear mapping of sample values
    // into the range appropriate for the function's output values. Default
    // value: same as the value of Range
    private COSArray decode = null;

    /**
     * Constructor using the given shading dictionary.
     * @param shadingDictionary the dictionary for this shading
     */
    public PDShadingType4(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    @Override
    public int getShadingType()
    {
        return PDShading.SHADING_TYPE4;
    }

    /**
     * The bits per component of this shading. This will return -1 if one has not been set.
     * @return the number of bits per component
     */
    public int getBitsPerComponent()
    {
        return getCOSDictionary().getInt(COSName.BITS_PER_COMPONENT, -1);
    }

    /**
     * Set the number of bits per component.
     * @param bitsPerComponent the number of bits per component
     */
    public void setBitsPerComponent(int bitsPerComponent)
    {
        getCOSDictionary().setInt(COSName.BITS_PER_COMPONENT, bitsPerComponent);
    }

    /**
     * The bits per coordinate of this shading. This will return -1 if one has not been set.
     * @return the number of bits per coordinate
     */
    public int getBitsPerCoordinate()
    {
        return getCOSDictionary().getInt(COSName.BITS_PER_COORDINATE, -1);
    }

    /**
     * Set the number of bits per coordinate.
     * @param bitsPerComponent the number of bits per coordinate
     */
    public void setBitsPerCoordinate(int bitsPerComponent)
    {
        getCOSDictionary().setInt(COSName.BITS_PER_COORDINATE, bitsPerComponent);
    }

    /**
     * The bits per flag of this shading. This will return -1 if one has not been set.
     * @return The number of bits per flag.
     */
    public int getBitsPerFlag()
    {
        return getCOSDictionary().getInt(COSName.BITS_PER_FLAG, -1);
    }

    /**
     * Set the number of bits per flag.
     * @param bitsPerFlag the number of bits per flag
     */
    public void setBitsPerFlag(int bitsPerFlag)
    {
        getCOSDictionary().setInt(COSName.BITS_PER_FLAG, bitsPerFlag);
    }

    /**
     * Returns all decode values as COSArray.
     * @return the decode array
     */
    private COSArray getDecodeValues()
    {
        if (decode == null)
        {
            decode = (COSArray) getCOSDictionary().getDictionaryObject(COSName.DECODE);
        }
        return decode;
    }

    /**
     * This will set the decode values.
     * @param decodeValues the new decode values
     */
    public void setDecodeValues(COSArray decodeValues)
    {
        decode = decodeValues;
        getCOSDictionary().setItem(COSName.DECODE, decodeValues);
    }

    /**
     * Get the decode for the input parameter.
     * @param paramNum the function parameter number
     * @return the decode parameter range or null if none is set
     */
    public PDRange getDecodeForParameter(int paramNum)
    {
        PDRange retval = null;
        COSArray decodeValues = getDecodeValues();
        if (decodeValues != null && decodeValues.size() >= paramNum * 2 + 1)
        {
            retval = new PDRange(decodeValues, paramNum);
        }
        return retval;
    }

    @Override
    public Paint toPaint(Matrix matrix, int pageHeight)
    {
        return new Type4ShadingPaint(this, matrix, pageHeight);
    }
}
