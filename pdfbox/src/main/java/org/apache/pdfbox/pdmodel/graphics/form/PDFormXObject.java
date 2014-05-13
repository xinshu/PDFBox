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
package org.apache.pdfbox.pdmodel.graphics.form;

import java.awt.geom.AffineTransform;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.util.Matrix;

/*
TODO There are further Form XObjects to implement:

+ PDFormXObject
|- PDReferenceXObject
|- PDGroupXObject
   |- PDTransparencyXObject

See PDF 32000 p111

When doing this all methods on PDFormXObject should probably be made
final and all fields private.
*/

/**
 * A Form XObject.
 * 
 * @author Ben Litchfield
 */
public final class PDFormXObject extends PDXObject
{
    private static final Log LOG = LogFactory.getLog(PDFormXObject.class);

    // name of XObject in resources, to prevent recursion
    private String name;

    /**
     * Creates a Form XObject for reading.
     * @param stream The XObject stream
     */
    public PDFormXObject(PDStream stream)
    {
        super(stream, COSName.FORM);
    }

    /**
     * Creates a Form XObject for reading.
     * @param stream The XObject stream
     * @param name The name of the form XObject, to prevent recursion.
     */
    public PDFormXObject(PDStream stream, String name)
    {
        super(stream, COSName.FORM);
        this.name = name;
    }

    /**
     * Creates a Form Image XObject for writing, in the given document.
     * @param document The current document
     */
    public PDFormXObject(PDDocument document)
    {
        super(document, COSName.FORM);
    }

    /**
     * This will get the form type, currently 1 is the only form type.
     * @return The form type.
     */
    public int getFormType()
    {
        return getCOSStream().getInt(COSName.FORMTYPE, 1);
    }

    /**
     * Set the form type.
     * @param formType The new form type.
     */
    public void setFormType(int formType)
    {
        getCOSStream().setInt(COSName.FORMTYPE, formType);
    }

    /**
     * This will get the resources at this page and not look up the hierarchy.
     * This attribute is inheritable, and findResources() should probably used.
     * This will return null if no resources are available at this level.
     * @return The resources at this level in the hierarchy.
     */
    public PDResources getResources()
    {
        PDResources retval = null;
        COSDictionary resources = (COSDictionary) getCOSStream().getDictionaryObject(COSName.RESOURCES);
        if (resources != null)
        {
            retval = new PDResources(resources);
            // check for a recursion, see PDFBOX-1813
            if (name != null)
            {
                Map<String, PDXObject> xobjects = retval.getXObjects();
                if (xobjects != null && xobjects.containsKey(name))
                {
                    PDXObject xobject = xobjects.get(name);
                    if (xobject instanceof PDFormXObject)
                    {
                        int length1 = getCOSStream().getInt(COSName.LENGTH);
                        int length2 = xobject.getCOSStream().getInt(COSName.LENGTH);
                        // seems to be the same object
                        if (length1 == length2)
                        {
                            retval.removeXObject(name);
                            LOG.debug("Removed XObjectForm "+name+" to avoid a recursion");
                        }
                    }
                }
            }
        }
        return retval;
    }

    /**
     * This will set the resources for this page.
     * @param resources The new resources for this page.
     */
    public void setResources(PDResources resources)
    {
        getCOSStream().setItem(COSName.RESOURCES, resources);
    }

    /**
     * An array of four numbers in the form coordinate system (see below),
     * giving the coordinates of the left, bottom, right, and top edges, respectively,
     * of the form XObject's bounding box.
     * These boundaries are used to clip the form XObject and to determine its size for caching.
     * @return The BBox of the form.
     */
    public PDRectangle getBBox()
    {
        PDRectangle retval = null;
        COSArray array = (COSArray) getCOSStream().getDictionaryObject(COSName.BBOX);
        if (array != null)
        {
            retval = new PDRectangle(array);
        }
        return retval;
    }

    /**
     * This will set the BBox (bounding box) for this form.
     * @param bbox The new BBox for this form.
     */
    public void setBBox(PDRectangle bbox)
    {
        if (bbox == null)
        {
            getCOSStream().removeItem(COSName.BBOX);
        }
        else
        {
            getCOSStream().setItem(COSName.BBOX, bbox.getCOSArray());
        }
    }

    /**
     * This will get the optional Matrix of an XObjectForm. It maps the form space into the user space
     * @return the form matrix
     */
    public Matrix getMatrix()
    {
        Matrix retval = null;
        COSArray array = (COSArray) getCOSStream().getDictionaryObject(COSName.MATRIX);
        if (array != null)
        {
            retval = new Matrix();
            retval.setValue(0, 0, ((COSNumber) array.get(0)).floatValue());
            retval.setValue(0, 1, ((COSNumber) array.get(1)).floatValue());
            retval.setValue(1, 0, ((COSNumber) array.get(2)).floatValue());
            retval.setValue(1, 1, ((COSNumber) array.get(3)).floatValue());
            retval.setValue(2, 0, ((COSNumber) array.get(4)).floatValue());
            retval.setValue(2, 1, ((COSNumber) array.get(5)).floatValue());
        }
        return retval;
    }

    /**
     * Sets the optional Matrix entry for the form XObject.
     * @param transform the transformation matrix
     */
    public void setMatrix(AffineTransform transform)
    {
        COSArray matrix = new COSArray();
        double[] values = new double[6];
        transform.getMatrix(values);
        for (double v : values)
        {
            matrix.add(new COSFloat((float) v));
        }
        getCOSStream().setItem(COSName.MATRIX, matrix);
    }

    /**
     * This will get the key of this XObjectForm in the structural parent tree.
     * Required if the form XObject contains marked-content sequences that are
     * structural content items.
     * @return the integer key of the XObjectForm's entry in the structural parent tree
     */
    public int getStructParents()
    {
        return getCOSStream().getInt(COSName.STRUCT_PARENTS, 0);
    }

    /**
     * This will set the key for this XObjectForm in the structural parent tree.
     * @param structParent The new key for this XObjectForm.
     */
    public void setStructParents(int structParent)
    {
        getCOSStream().setInt(COSName.STRUCT_PARENTS, structParent);
    }
}
