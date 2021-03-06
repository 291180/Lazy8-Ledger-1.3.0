/*
 * BufferPrinter1_3.java - Main class that controls printing
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * changed 2002-04-28/Thomas Dilts added print preview and 
 *                    saved all values of pageSetup to disk
 *                    Added java 1.4 functions
 */

//package org.gjt.sp.jedit.print;
package org.lazy8.nu.ledger.print;
import org.lazy8.nu.util.gen.Fileio;

//{{{ Imports
import java.awt.print.*;
import java.awt.*;
import java.io.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

public class BufferPrinter1_4
{

	//{{{ print() method
	public static void print(View view, Buffer buffer, boolean selection)
	{
		PrinterJob job =getPrintJob(buffer.getPath());
		boolean header = jEdit.getBooleanProperty("print.header");
		boolean footer = jEdit.getBooleanProperty("print.footer");
		boolean lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		boolean color = jEdit.getBooleanProperty("print.color");
		Font font = jEdit.getFontProperty("print.font");

		job.setPrintable(new BufferPrintable(buffer,font,header,footer,
		                                     lineNumbers,color));

		if(!job.printDialog(format))
			return;
		savePrintSpec();

		try
		{
			job.print(format);
		}
		catch(PrinterAbortException ae)
		{
			Log.log(Log.DEBUG,BufferPrinter1_4.class,ae);
		}
		catch(PrinterException e)
		{
			Log.log(Log.ERROR,BufferPrinter1_4.class,e);
			String[] args = { e.toString() };
			GUIUtilities.error(view,"print-error",args);
		}
	} //}}}

	//{{{ printPreview() method
	public static void printPreview(View view,Buffer buffer, boolean selection)
	{
		boolean header = jEdit.getBooleanProperty("print.header");
		boolean footer = jEdit.getBooleanProperty("print.footer");
		boolean lineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		boolean color = jEdit.getBooleanProperty("print.color");
		Font font = jEdit.getFontProperty("print.font");
		PrinterJob prnJob=getPrintJob(buffer.getPath());
		PrintPreview frame = new PrintPreview( view, buffer,
		                                       selection,new BufferPrintable(buffer,font,header,footer,
		                                                                     lineNumbers,color), prnJob,
										     getPageFormat());
		frame.setVisible(true);
	}
	//}}}

	//{{{ pageSetup() method
	public static void pageSetup(View view)
	{
		PrinterJob prnJob=getPrintJob("PageSetupOnly");
		if(prnJob.pageDialog(format)!=null)
			savePrintSpec();
	} //}}}

	//{{{ getPageFormat() method
	public static PageFormat getPageFormat()
	{
		//convert from PrintRequestAttributeSet to the pageFormat
		PrinterJob prnJob=getPrintJob(" ");
		PageFormat pf=prnJob.defaultPage();
		Paper pap=pf.getPaper();

		MediaSizeName media=(MediaSizeName)format.get(
		                            Media.class);
		MediaSize ms=MediaSize.getMediaSizeForName(media);

		MediaPrintableArea mediaarea=(MediaPrintableArea)format.get(
		                                     MediaPrintableArea.class);
		if(mediaarea!=null)
			pap.setImageableArea((double)(mediaarea.getX(MediaPrintableArea.INCH)*72),
			                     (double)(mediaarea.getY(MediaPrintableArea.INCH)*72),
			                     (double)(mediaarea.getWidth(MediaPrintableArea.INCH)*72),
			                     (double)(mediaarea.getHeight(MediaPrintableArea.INCH)*72));
		if(ms!=null)
			pap.setSize((double)(ms.getX(MediaSize.INCH)*72),
			            (double)(ms.getY(MediaSize.INCH)*72));
		pf.setPaper(pap);

		OrientationRequested orientation=(OrientationRequested)format.get(
		                                         OrientationRequested.class);
		if(orientation!=null)
		{
			if(orientation.getValue()==OrientationRequested.LANDSCAPE.getValue())
			{
				pf.setOrientation(PageFormat.LANDSCAPE);
			}
			else if(orientation.getValue()==OrientationRequested.REVERSE_LANDSCAPE.getValue())
			{
				pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
			}
			else if(orientation.getValue()==OrientationRequested.PORTRAIT.getValue())
			{
				pf.setOrientation(PageFormat.PORTRAIT);
			}
			else if(orientation.getValue()==OrientationRequested.REVERSE_PORTRAIT.getValue())
			{
				//doesnt exist??
				//pf.setOrientation(PageFormat.REVERSE_PORTRAIT);
				//then just do the next best thing
				pf.setOrientation(PageFormat.PORTRAIT);
			}
		}
		return pf;
	} //}}}

	//{{{ savePrintSpec() method
	private static void savePrintSpec()
	{
		///////////////////////JeditCORE start//////////////////////////////////////////
		String printSpecPath = MiscUtilities.constructPath(
		                               jEdit.getSettingsDirectory(), "printspec");
		File filePrintSpec = new File(printSpecPath);
		///////////////////////JeditCORE end//////////////////////////////////////////
		///////////////////////Lazy8 start//////////////////////////////////////////
		/*
		File filePrintSpec=null;
		try{
			filePrintSpec=Fileio.getFile("printspec", "props", true, false);
		}catch(Exception e){
			Log.log(Log.ERROR,filePrintSpec,"Couldnt write file, error="+ e);
		}
		*/
		///////////////////////Lazy8 end//////////////////////////////////////////
		
		if ( filePrintSpec!=null && !filePrintSpec.isDirectory())
		{
			try
			{
				FileOutputStream fileOut=new FileOutputStream(filePrintSpec);
				ObjectOutputStream obOut=new ObjectOutputStream(fileOut);
				obOut.writeObject(format);
				//for backwards compatibility, the color variable is stored also as a property
				Chromaticity cc=(Chromaticity)format.get(Chromaticity.class);
				if (cc!=null)
					jEdit.setBooleanProperty("print.color",
						cc.getValue()==Chromaticity.COLOR.getValue());
			}
			catch(Exception ee)
			{
				Log.log(Log.ERROR,format,"Couldnt write to file="+filePrintSpec+"; error="+ ee);
			}
		}
	}
	//}}}

	//{{{ getPrintJob() method
	private static PrinterJob getPrintJob(String jobName)
	{
		PrinterJob job = PrinterJob.getPrinterJob();
		
		///////////////////////JeditCORE start//////////////////////////////////////////
		String printSpecPath = MiscUtilities.constructPath(
		                               jEdit.getSettingsDirectory(), "printspec");
		File filePrintSpec = new File(printSpecPath);
		
		///////////////////////JeditCORE end//////////////////////////////////////////
		///////////////////////Lazy8 start//////////////////////////////////////////
		/*File filePrintSpec=null;
		try{
			filePrintSpec=Fileio.getFile("printspec", "props", true, false);
		}catch(Exception e){
			Log.log(Log.ERROR,filePrintSpec,"Couldnt read file, error="+ e);
		}*/
		///////////////////////Lazy8 end//////////////////////////////////////////

		format = new HashPrintRequestAttributeSet();
		if (filePrintSpec!=null && filePrintSpec.exists() && !filePrintSpec.isDirectory()){

			try
			{
				FileInputStream fileIn=new FileInputStream(filePrintSpec);
				ObjectInputStream obIn=new ObjectInputStream(fileIn);
				format=(HashPrintRequestAttributeSet)obIn.readObject();
			}
			catch(Exception ee)
			{
				Log.log(Log.ERROR,job,"Couldnt read file="+filePrintSpec+"; error="+ ee);
			}
			//for backwards compatibility, the color variable is stored also as a property
			if(jEdit.getBooleanProperty("print.color"))
				format.add(Chromaticity.COLOR);
			else
				format.add(Chromaticity.MONOCHROME);

			//no need to always keep the same job name for every printout.
			format.add(new JobName(jobName, null));

		}
		return job;

	}
	//}}}

	//{{{ Private members
	private static PrintRequestAttributeSet format;
	//}}}
}

