package solution;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;

import mmlib4j.filtering.residual.ultimateLevelings.UltimateAttributeOpening;
import mmlib4j.images.ColorImage;
import mmlib4j.images.GrayScaleImage;
import mmlib4j.representation.tree.NodeLevelSets;
import mmlib4j.representation.tree.attribute.Attribute;
import mmlib4j.representation.tree.attribute.AttributeComputedIncrementally;
import mmlib4j.representation.tree.componentTree.ComponentTree;
import mmlib4j.representation.tree.componentTree.ConnectedFilteringByComponentTree;
import mmlib4j.representation.tree.componentTree.NodeCT;
import mmlib4j.representation.tree.pruningStrategy.PruningBasedAttribute;
import mmlib4j.representation.tree.pruningStrategy.PruningBasedMSER;
import mmlib4j.representation.tree.pruningStrategy.PruningBasedMumfordShahEnergy;
import mmlib4j.representation.tree.pruningStrategy.PruningBasedTBMR;
import mmlib4j.segmentation.ThresholdGlobal;
import mmlib4j.utils.AdjacencyRelation;
import mmlib4j.utils.ImageBuilder;
import mmlib4j.utils.Utils;


public class Solution {
	
	public static final int MUMFORD_SHAH = 0;
	
	public static final int MSER = 1;
	
	public static final int TBMR = 2;
	
	private int c = 1;
	
	private int typeFilter;
	
	private String saveResultsPath;
	
	private boolean saveImageResult = false;
	
	private boolean saveImageBounds = false;
	
	public Solution( int typeFilter, String saveResultsPath ) {
		
		this.typeFilter = typeFilter;
		
		this.saveResultsPath = saveResultsPath;
		
	}
	
	public void run( String root,			
			 		 String dataSetsInputPath,
			 		 String dataSet,
			 		 int filterParameter,
			 		 double distanceRgb,
			 		 double distance,
			 		 int areaFilter) throws Exception {		
		
		getNextValidDir( root + saveResultsPath + "result-" , "/" + dataSet, String.valueOf(filterParameter) );
		
		File dir = new File( dataSetsInputPath + dataSet );
		
		for( File file : dir.listFiles() ) {
			
			if( !file.getName().endsWith( "_rgb.png" ) ) continue;
			
			run( file, root, dataSet, filterParameter, distanceRgb, distance, areaFilter );
			
		}
		
	}
	
	private void run( File file, 
					  String root, 
					  String dataSet,
					  int filterParameter,
					  double distanceRgb,
					  double distance,
					  int areaFilter) throws Exception  {
		
		System.err.println( "Processing image: " + file.getName() );
		
		ColorImage imgRGB = ImageBuilder.openRGBImage( file );
		
		GrayScaleImage img = imgRGB.getGreen();
		
		ConnectedFilteringByComponentTree maxtree = new ConnectedFilteringByComponentTree( img, AdjacencyRelation.getAdjacency8(), true);
		
		UltimateAttributeOpening uao = new UltimateAttributeOpening( maxtree );
		
		PruningBasedAttribute pruning = new PruningBasedAttribute( maxtree, Attribute.AREA );
		
		boolean mapFilter[];
		
		switch ( typeFilter ) {
		
			case MUMFORD_SHAH:
				mapFilter = filterByMumfordShah( maxtree, imgRGB, filterParameter, distanceRgb, areaFilter );
				break;				
				
			case MSER:
				mapFilter = filterByMser( maxtree, imgRGB, filterParameter, distanceRgb, areaFilter );
				break;
				
			default :
				mapFilter = filterByTbmr( maxtree, imgRGB, filterParameter, distanceRgb, areaFilter );
				break;
				
		}		
		
		uao.computeUAO( 50000, Attribute.AREA, pruning.getMappingSelectedNodes(), mapFilter );			
		
		GrayScaleImage imgBinQ = ThresholdGlobal.upperSet( uao.getAssociateIndexImage(), 1 ).convertGrayScale();
		
		ComponentTree treeBin = new ComponentTree( imgBinQ, AdjacencyRelation.getAdjacency8(), true );			
		
		// paths and files
		
		String path = file.getPath().toString();
		
		int index = path.lastIndexOf( "/" );
		
		// file index and root
		
		String fileIndex = path.substring( index, path.length() - 8 ); 					
		
		// directory to save test results
		
		String resultsDir = root + saveResultsPath + "result-" + c +"(filter=" + String.valueOf(filterParameter) + ")/" + dataSet;				
		
		FileWriter saida = new FileWriter( new File( resultsDir + fileIndex + "_bbox.csv" ) );								 	
		
		NodeCT [] nodesBoundeds = postProcessing( treeBin, distance );
	 	
		for( int i = 0 ; i < treeBin.getNumNode()-1 ; i++ ) {
	 		
			NodeCT node = nodesBoundeds[ i ];
	 		
			if( node != null ) {
	 		
				imgRGB.paintBoundBox( node.getXmin(), node.getYmin(), node.getXmax(), node.getYmax(), Color.RED.getRGB() );									
			
				saida.append( node.getXmin() + "," + node.getYmin() + "," );
			  
				saida.append( node.getXmin() + "," + node.getYmax() + "," );
			
				saida.append( node.getXmax() + "," + node.getYmax() + "," );
			
				saida.append( node.getXmax() + "," + node.getYmin() + "\n" );
			
			}
	 		
		}		 			 	
		
		saida.close();
		
		if( saveImageResult )
			ImageBuilder.saveImage( ThresholdGlobal.upperSet( imgBinQ, 1 ), new File( resultsDir + fileIndex + "_BIN.png" ) );
		
		if( saveImageBounds )
			ImageBuilder.saveImage( imgRGB, new File( resultsDir + fileIndex + "_BOUNDS.png" ) );					
		
		nodesBoundeds = null;		
		
		imgBinQ = null;
		
		treeBin = null;
		
		maxtree = null;
		
		uao = null;
		
		pruning = null;
		
		imgRGB = null;
		
		img = null;
		
		mapFilter = null;
		
	}
	
	private void getNextValidDir( String resultsDir, String dataset, String filterParameter ) {
		
		File dir = new File( resultsDir + c + "(filter=" + filterParameter + ")" + dataset );				
		
		while( dir.exists() ) {
			
			c+=1;
			
			dir = new File( resultsDir + c + "(filter=" + filterParameter + ")" + dataset );
			
		}
			
		dir.mkdirs();
		
	}
	
	private NodeCT [] postProcessing( ComponentTree treeBin, double distance ) {
		
		long ti = System.currentTimeMillis();
		
		NodeCT [] nodesBoundeds = new NodeCT[ treeBin.getNumNode()-1 ];			 			 			 		 	
	 	
	 	int i = 0;		 
	 	
	 	for( NodeCT node: treeBin.getListNodes() ) {
	 		
	 		if( node != treeBin.getRoot() ) {		 
	 			
	 			nodesBoundeds[ i++ ] = node;		 			
	 				
	 		}
	 		
	 	}		 			 		 	 			 
		
	 	for( i = 0 ; i < treeBin.getNumNode()-1 ; i++ ) {
	 		
	 		for( int j = 0 ; j < treeBin.getNumNode()-1 ; j++ ) {
	 			
	 			if( i == j ) continue;
	 			
	 			NodeCT ni = nodesBoundeds[ i ];
	 			
	 			NodeCT nj = nodesBoundeds[ j ];			 					 			
	 				
	 			if( ni != null && nj != null && isMerged( ni, nj, distance ) ) {	
	 				
	 				for( int p : nj.getCanonicalPixels() ) {
 						
 						ni.addPixel( p ); 											
 					} 					
 					
 					nodesBoundeds[ j ] = null;	 					 			 				
	 			}		 					 				
	 			
	 		}
	 		
	 	}
	 	
	 	long tf = System.currentTimeMillis();
		
	 	if( Utils.debug ) {
	 		
	 		System.out.println( "Tempo de execucao [post processing]  " + ( ( tf - ti ) / 1000.0 )  + "s" );
	 		
	 	}
	 	
	 	return nodesBoundeds;
		
	}
	
	private boolean isMerged( NodeCT ni, NodeCT nj, double distance ) {
		
		int xci = ( ni.getXmax() + ni.getXmin() ) / 2;
		
		int yci = ( ni.getYmax() + ni.getYmin() ) / 2;
		
		int xcj = ( nj.getXmax() + nj.getXmin() ) / 2;
		
		int ycj = ( nj.getYmax() + nj.getYmin() ) / 2;		
		
		// Ara2013 - RPI -> 100
		
		// Ara2013 - Canon -> 250
		
		// Ara2012 - 250
		
		if( Math.sqrt( Math.pow( xci - xcj, 2 ) + Math.pow( yci - ycj, 2 ) ) < distance  ) {					
			
			return true;
			
		}
		
		return false;
		
	}
	
	public double[][] computerColorAttribute( ConnectedFilteringByComponentTree maxtree, final ColorImage imgRGB ){
		
		final int somaRGB[][] = new int[ maxtree.getNumNode() ][ 3 ];
		
		final double distRGB[][] = new double[ maxtree.getNumNode() ][ 2 ];		
		
		new AttributeComputedIncrementally() {						
			
			int [][] rgbRefTrue = { { 126, 202, 0 }, {90,161,0}, {112,155,0}, {145, 225, 37}, {62,64,61} };
			
			int [][] rgbRefFalse = { { 70, 72, 72 }, {68,54,39}, {74, 79, 78}, {52, 54, 55}, {42,46,47} };			
			
			@Override
			public void preProcessing(NodeLevelSets v) {
				
				for(int p: v.getCanonicalPixels()){
					
					somaRGB[v.getId()][0] += imgRGB.getRed(p);
					somaRGB[v.getId()][1] += imgRGB.getGreen(p);
					somaRGB[v.getId()][2] += imgRGB.getBlue(p);
					
				}
				
			}
			@Override
			public void mergeChildren(NodeLevelSets parent, NodeLevelSets son) {
				
				somaRGB[parent.getId()][0] += somaRGB[son.getId()][0];
				somaRGB[parent.getId()][1] += somaRGB[son.getId()][1];
				somaRGB[parent.getId()][2] += somaRGB[son.getId()][2];
				
			}

			@Override
			public void posProcessing(NodeLevelSets parent) { 
				
				double rm = somaRGB[parent.getId()][0] / (double) parent.getArea();
				double gm = somaRGB[parent.getId()][1] / (double) parent.getArea();
				double bm = somaRGB[parent.getId()][2] / (double) parent.getArea();				
				
				double sumRgbTrue = 0, sumRgbFalse = 0;
				
				for( int i = 0 ; i < rgbRefTrue.length-4 ; i++ ) {
					
					sumRgbTrue += Math.pow( rm-rgbRefTrue[i][0], 2 ) + Math.pow( gm-rgbRefTrue[i][1], 2 ) + Math.pow( bm-rgbRefTrue[i][2], 2 );
					
					sumRgbFalse += Math.pow( rm-rgbRefFalse[i][0], 2 ) + Math.pow( gm-rgbRefFalse[i][1], 2 ) + Math.pow( bm-rgbRefFalse[i][2], 2 );
					
				}
				
				distRGB[ parent.getId() ][ 0 ] = Math.sqrt( sumRgbTrue );
				
				distRGB[ parent.getId() ][ 1 ] = Math.sqrt( sumRgbFalse );
				
			}
			
		}.computerAttribute( maxtree.getRoot() );
		
		return distRGB;
		
	}
	
	public boolean [] filterByMumfordShah( ConnectedFilteringByComponentTree maxtree, ColorImage imgRGB, int minEnergy, double distanceRgb, int areaFilter ) {					
		
		PruningBasedMumfordShahEnergy mumfordShahEnergy = new PruningBasedMumfordShahEnergy( maxtree, minEnergy, true );		
		
		boolean mapMumfordShahEnergy[] = mumfordShahEnergy.getMappingSelectedNodes();
		
		boolean map[] = new boolean[ maxtree.getNumNode() ];
		
		double distRGB[][] = computerColorAttribute( maxtree, imgRGB );
		
		for( NodeCT node: maxtree.getListNodes() ) {
			
			map[ node.getId() ] = mapMumfordShahEnergy[ node.getId() ] && node.getArea() > areaFilter && distRGB[ node.getId() ][ 0 ] < distanceRgb;
			
		}
		
		return map;
		
	}
	
	public boolean [] filterByMser( ConnectedFilteringByComponentTree maxtree, ColorImage imgRGB, int delta, double distanceRgb, int areaFilter ) {					
		
		PruningBasedMSER pruningBasedMSER = new PruningBasedMSER( maxtree, delta );		
		
		boolean mapMser[] = pruningBasedMSER.getMappingSelectedNodes();
		
		boolean map[] = new boolean[ maxtree.getNumNode() ];
		
		double distRGB[][] = computerColorAttribute( maxtree, imgRGB );
		
		for( NodeCT node: maxtree.getListNodes() ) {															
			
			map[ node.getId() ] = mapMser[ node.getId() ] && node.getArea() > areaFilter && distRGB[ node.getId() ][ 0 ] < distanceRgb;
			
		}
		
		return map;
		
	}
	
	public boolean [] filterByTbmr( ConnectedFilteringByComponentTree maxtree, ColorImage imgRGB, int tmin, double distanceRgb, int areaFilter ) {					
		
		PruningBasedTBMR pruningBasedTBMR = new PruningBasedTBMR( maxtree, tmin, maxtree.getInputImage().getSize());		
		
		boolean mapTbmr[] = pruningBasedTBMR.getMappingSelectedNodes();
		
		boolean map[] = new boolean[ maxtree.getNumNode() ];
		
		double distRGB[][] = computerColorAttribute( maxtree, imgRGB );
		
		for( NodeCT node: maxtree.getListNodes() ) {
			
			map[ node.getId() ] = mapTbmr[ node.getId() ] && node.getArea() > areaFilter && distRGB[ node.getId() ][ 0 ] < distanceRgb;
			
		}
		
		return map;
		
	}
	
	public boolean isSaveImageResult() {
		return saveImageResult;
	}

	public void setSaveImageResult(boolean saveImageResult) {
		this.saveImageResult = saveImageResult;
	}

	public boolean isSaveImageBounds() {
		return saveImageBounds;
	}

	public void setSaveImageBounds(boolean saveImageBounds) {
		this.saveImageBounds = saveImageBounds;
	}


}
