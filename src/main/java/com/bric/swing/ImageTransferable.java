/*
 * @(#)ImageTransferable.java
 *
 * $Date: 2009-02-20 08:34:41 +0100 (P, 20 febr. 2009) $
 *
 * Copyright (c) 2009 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.blogspot.com/
 * 
 * And the latest version should be available here:
 * https://javagraphics.dev.java.net/
 */
package com.bric.swing;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class ImageTransferable implements Transferable {
	Image img;
	
	public ImageTransferable(Image i) {
		img = i;
	}

	public Object getTransferData(DataFlavor f)
			throws UnsupportedFlavorException, IOException {
		if(f.equals(DataFlavor.imageFlavor)==false)
			throw new UnsupportedFlavorException(f);
		return img;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {DataFlavor.imageFlavor};
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return(flavor.equals(DataFlavor.imageFlavor));
	}
	
}
