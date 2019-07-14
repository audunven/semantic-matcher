package mismatchdetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

// *****************************************************************************
//
// Copyright (c) 2011 Christian Meilicke (University of Mannheim)
//
// Permission is hereby granted, free of charge, to any person
// obtaining a copy of this software and associated documentation
// files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge,
// publish, distribute, sublicense, and/or sell copies of the Software,
// and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// *********************************************************************************

import java.util.ArrayList;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import de.unima.alcomox.ExtractionProblem;
import de.unima.alcomox.Settings;
import de.unima.alcomox.exceptions.AlcomoException;
import de.unima.alcomox.exceptions.MappingException;
import de.unima.alcomox.mapping.Characteristic;
import de.unima.alcomox.mapping.Correspondence;
import de.unima.alcomox.mapping.Mapping;
import de.unima.alcomox.ontology.IOntology;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;



/**
 * This example illustrates the usage of ALCOMO to repair an
 * incoherent alignment. It is shows a standard usage of the system.
 * 
 */
public class IncoherenceDetection {
	

	public static URIAlignment detectIncoherenceMismatch (String onto1File, String onto2File, String inputAlignmentPath) throws AlcomoException, AlignmentException {
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(new File(inputAlignmentPath).toURI().toString());
		
		URIAlignment coherentAlignment = new URIAlignment();

		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		coherentAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		// we ant to use Pellet as reasoner (alternatively use HERMIT)
		Settings.BLACKBOX_REASONER = Settings.BlackBoxReasoner.PELLET;

		// if you want to force to generate a one-to-one alignment add this line
		// by default its set to false
		Settings.ONE_TO_ONE = false;

		// load ontologies as IOntology (uses fast indexing for efficient reasoning)
		// formerly LocalOntology now IOntology is recommended
		IOntology sourceOnt = new IOntology(onto1File);
		IOntology targetOnt = new IOntology(onto2File);

		// load the mapping
		Mapping mapping = new Mapping(inputAlignmentPath);

		// define diagnostic problem
		ExtractionProblem ep = new ExtractionProblem(
				ExtractionProblem.ENTITIES_CONCEPTSPROPERTIES,
				ExtractionProblem.METHOD_OPTIMAL,
				ExtractionProblem.REASONING_COMPLETE
				);

		// attach ontologies and mapping to the problem
		ep.bindSourceOntology(sourceOnt);
		ep.bindTargetOntology(targetOnt);
		ep.bindMapping(mapping);

		// solve the problem
		ep.solve();

		Mapping extracted = ep.getExtractedMapping();


		for (Correspondence c : extracted.getCorrespondences()) {

			coherentAlignment.addAlignCell(URI.create(c.getSourceEntityUri()), URI.create(c.getTargetEntityUri()), c.getRelation().toString(), c.getConfidence());

		}

		//System.out.println("removed the following correspondences:\n");

		//for (Correspondence c : ep.getDiscardedMapping().getCorrespondences()) {
		//	System.out.println(c.toString());
		//}

		return coherentAlignment;

	}


}
