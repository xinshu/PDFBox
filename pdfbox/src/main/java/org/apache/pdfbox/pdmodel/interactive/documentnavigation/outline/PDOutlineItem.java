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
package org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionFactory;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

/**
 * This represents an outline in a pdf document.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 */
public class PDOutlineItem extends PDOutlineNode
{
    private static final int ITALIC_FLAG = 1;
    private static final int BOLD_FLAG = 2;

    /**
     * Default Constructor.
     */
    public PDOutlineItem()
    {
        super();
    }

    /**
     * Constructor for an existing outline item.
     *
     * @param dic The storage dictionary.
     */
    public PDOutlineItem( COSDictionary dic )
    {
        super( dic );
    }

    /**
     * Insert a sibling after this node.
     *
     * @param item The item to insert.
     */
    public void insertSiblingAfter( PDOutlineItem item )
    {
        item.setParent( getParent() );
        PDOutlineItem next = getNextSibling();
        setNextSibling( item );
        item.setPreviousSibling( this );
        if( next != null )
        {
            item.setNextSibling( next );
            next.setPreviousSibling( item );
        }
        updateParentOpenCount( 1 );
    }

    /**
     * {@inheritDoc}
     */
    public PDOutlineNode getParent()
    {
        return super.getParent();
    }

    /**
     * Return the previous sibling or null if there is no sibling.
     *
     * @return The previous sibling.
     */
    public PDOutlineItem getPreviousSibling()
    {
        PDOutlineItem last = null;
        COSDictionary lastDic = (COSDictionary)node.getDictionaryObject( COSName.PREV );
        if( lastDic != null )
        {
            last = new PDOutlineItem( lastDic );
        }
        return last;
    }

    /**
     * Set the previous sibling, this will be maintained by this class.
     *
     * @param outlineNode The new previous sibling.
     */
    protected void setPreviousSibling( PDOutlineNode outlineNode )
    {
        node.setItem( COSName.PREV, outlineNode );
    }

    /**
     * Return the next sibling or null if there is no next sibling.
     *
     * @return The next sibling.
     */
    public PDOutlineItem getNextSibling()
    {
        PDOutlineItem last = null;
        COSDictionary lastDic = (COSDictionary)node.getDictionaryObject( COSName.NEXT );
        if( lastDic != null )
        {
            last = new PDOutlineItem( lastDic );
        }
        return last;
    }

    /**
     * Set the next sibling, this will be maintained by this class.
     *
     * @param outlineNode The new next sibling.
     */
    protected void setNextSibling( PDOutlineNode outlineNode )
    {
        node.setItem( COSName.NEXT, outlineNode );
    }

    /**
     * Get the title of this node.
     *
     * @return The title of this node.
     */
    public String getTitle()
    {
        return node.getString( COSName.TITLE );
    }

    /**
     * Set the title for this node.
     *
     * @param title The new title for this node.
     */
    public void setTitle( String title )
    {
        node.setString( COSName.TITLE, title );
    }

    /**
     * Get the page destination of this node.
     *
     * @return The page destination of this node.
     * @throws IOException If there is an error creating the destination.
     */
    public PDDestination getDestination() throws IOException
    {
        return PDDestination.create( node.getDictionaryObject( COSName.DEST ) );
    }

    /**
     * Set the page destination for this node.
     *
     * @param dest The new page destination for this node.
     */
    public void setDestination( PDDestination dest )
    {
        node.setItem( COSName.DEST, dest );
    }

    /**
     * A convenience method that will create an XYZ destination using only the defaults.
     *
     * @param page The page to refer to.
     */
    public void setDestination( PDPage page )
    {
        PDPageXYZDestination dest = null;
        if( page != null )
        {
            dest = new PDPageXYZDestination();
            dest.setPage( page );
        }
        setDestination( dest );
    }

    /**
     * This method will attempt to find the page in this PDF document that this outline points to.
     * If the outline does not point to anything then this method will return null.  If the outline
     * is an action that is not a GoTo action then this methods will throw the OutlineNotLocationException
     *
     * @param doc The document to get the page from.
     *
     * @return The page that this outline will go to when activated or null if it does not point to anything.
     * @throws IOException If there is an error when trying to find the page.
     */
    public PDPage findDestinationPage( PDDocument doc ) throws IOException
    {
        PDDestination dest = getDestination();
        if( dest == null )
        {
            PDAction outlineAction = getAction();
            if( outlineAction instanceof PDActionGoTo )
            {
                dest = ((PDActionGoTo)outlineAction).getDestination();
            }
            else
            {
                return null;
            }
        }

        PDPageDestination pageDestination;
        if( dest instanceof PDNamedDestination )
        {
            //if we have a named destination we need to lookup the PDPageDestination
            PDNamedDestination namedDest = (PDNamedDestination)dest;
            PDDocumentNameDictionary namesDict = doc.getDocumentCatalog().getNames();
            if( namesDict != null )
            {
                PDDestinationNameTreeNode destsTree = namesDict.getDests();
                if( destsTree != null )
                {
                    pageDestination = (PDPageDestination)destsTree.getValue( namedDest.getNamedDestination() );
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
        else if( dest instanceof PDPageDestination)
        {
            pageDestination = (PDPageDestination) dest;
        }
        else if( dest == null )
        {
            return null;
        }
        else
        {
            throw new IOException( "Error: Unknown destination type " + dest );
        }

        PDPage page = pageDestination.getPage();
        if( page == null )
        {
            int pageNumber = pageDestination.getPageNumber();
            if( pageNumber != -1 )
            {
                List allPages = doc.getDocumentCatalog().getAllPages();
                page = (PDPage)allPages.get( pageNumber );
            }
        }

        return page;
    }

    /**
     * Get the action of this node.
     *
     * @return The action of this node.
     */
    public PDAction getAction()
    {
        return PDActionFactory.createAction( (COSDictionary)node.getDictionaryObject( COSName.A ) );
    }

    /**
     * Set the action for this node.
     *
     * @param action The new action for this node.
     */
    public void setAction( PDAction action )
    {
        node.setItem( COSName.A, action );
    }

    /**
     * Get the structure element of this node.
     *
     * @return The structure element of this node.
     */
    public PDStructureElement getStructureElement()
    {
        PDStructureElement se = null;
        COSDictionary dic = (COSDictionary)node.getDictionaryObject( COSName.SE );
        if( dic != null )
        {
            se = new PDStructureElement( dic );
        }
        return se;
    }

    /**
     * Set the structure element for this node.
     *
     * @param structureElement The new structure element for this node.
     */
    public void setStructuredElement( PDStructureElement structureElement )
    {
        node.setItem( COSName.SE, structureElement );
    }

    /**
     * Get the RGB text color of this node.  Default is black and this method
     * will never return null.
     *
     * @return The structure element of this node.
     */
    public PDColor getTextColor()
    {
        PDColor retval = null;
        COSArray csValues = (COSArray)node.getDictionaryObject( COSName.C );
        if( csValues == null )
        {
            csValues = new COSArray();
            csValues.growToSize( 3, new COSFloat( 0 ) );
            node.setItem( COSName.C, csValues );
        }
        retval = new PDColor(csValues.toFloatArray());
        return retval;
    }

    /**
     * Set the RGB text color for this node.
     *
     * @param textColor The text color for this node.
     */
    public void setTextColor( PDColor textColor )
    {
        node.setItem( COSName.C, textColor.toCOSArray() );
    }

    /**
     * Set the RGB text color for this node.
     *
     * @param textColor The text color for this node.
     */
    public void setTextColor( Color textColor )
    {
        COSArray array = new COSArray();
        array.add( new COSFloat( textColor.getRed()/255f));
        array.add( new COSFloat( textColor.getGreen()/255f));
        array.add( new COSFloat( textColor.getBlue()/255f));
        node.setItem( COSName.C, array );
    }

    /**
     * A flag telling if the text should be italic.
     *
     * @return The italic flag.
     */
    public boolean isItalic()
    {
        return node.getFlag( COSName.F, ITALIC_FLAG );
    }

    /**
     * Set the italic property of the text.
     *
     * @param italic The new italic flag.
     */
    public void setItalic( boolean italic )
    {
        node.setFlag( COSName.F, ITALIC_FLAG, italic );
    }

    /**
     * A flag telling if the text should be bold.
     *
     * @return The bold flag.
     */
    public boolean isBold()
    {
        return node.getFlag( COSName.F, BOLD_FLAG );
    }

    /**
     * Set the bold property of the text.
     *
     * @param bold The new bold flag.
     */
    public void setBold( boolean bold )
    {
        node.setFlag( COSName.F, BOLD_FLAG, bold );
    }

}
