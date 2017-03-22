/** 
 * Author: Daniel R. Schlegel
 * Modified: March 21, 2017
 * 
 * Translates OWL to the Brat Normalization Format. This format is rather simple: 
 * ID <TAB> TYPE1:LABEL1:STRING1 <TAB> TYPE2:LABEL2:STRING2 [...]
 * e.g.: "50615   name:Name:faceted volume        name:Synonym:polyhedron" from FMA.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package owltools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OWLToBratNormalizationFormat {

	public static String bratifyLabel(OWLLiteral label){
		return "name:Name:" + label.getLiteral().replaceAll("[\\t\\n\\r]"," ");
	}
	
	public static String bratifySynonym(OWLLiteral syn){
		return "name:Synonym:" + syn.getLiteral().replaceAll("[\\t\\n\\r]"," ");
	}
	
	public static String bratifyDefinition(OWLLiteral def){
		return "info:Definition:" + def.getLiteral().replaceAll("[\\t\\n\\r]"," ");
	}
	
	public static String bratifyAttribute(String attrlabel, OWLLiteral attr){
		return "attr:" + attrlabel + ":" + attr.getLiteral().replaceAll("[\\t\\n\\r]"," ");
	}
	
	public static String bratifyClass(OWLClass owlclass, OWLOntology ont){
		IRI iri = owlclass.getIRI();
		
		String ret = iri.getShortForm();
		
		if(ret.contains("ObsoleteClass")) return "";
		
		for(OWLAnnotationAssertionAxiom a : ont.getAnnotationAssertionAxioms(iri)) {
			// Make sure class is not deprecated. 
			if(a.getProperty().isDeprecated()){
				if(a.getValue() instanceof OWLLiteral && 
						((OWLLiteral)a.getValue()).getLiteral().equals("true"))
					return "";
			}
			else if(a.getProperty().isLabel()) {
		        if(a.getValue() instanceof OWLLiteral) {
		        	ret += '\t' + bratifyLabel((OWLLiteral) a.getValue());
		        }
		    }
		    else if(a.getProperty().getIRI().toString().toLowerCase().contains("synonym") &&
		    		!a.getProperty().getIRI().toString().toLowerCase().contains("citation") &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("editor") &&
		    		!a.getProperty().getIRI().toString().toLowerCase().contains("source") &&
					a.getValue() instanceof OWLLiteral ){
		    	ret += '\t' + bratifySynonym((OWLLiteral) a.getValue());
			}
			
			// Adapted from ontology definition evaluation paper.
		    else if((a.getProperty().getIRI().toString().contains("IAO_0000115") || // Definition
					a.getProperty().getIRI().toString().contains("IAO_0000600") || // Elucidation
		    		a.getProperty().getIRI().toString().toLowerCase().contains("def")) &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("defines") &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("defined") &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("citation") &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("editor") &&
					!a.getProperty().getIRI().toString().toLowerCase().contains("source") &&
					a.getValue() instanceof OWLLiteral ){
		    	ret += '\t' + bratifyDefinition((OWLLiteral) a.getValue());
			}
		    else if(a.getValue() instanceof OWLLiteral){
		    	ret += '\t' + bratifyAttribute(a.getProperty().getIRI().getShortForm(), ((OWLLiteral)a.getValue()));
		    }
		}
		
		if(ret.contains("\t"))
			return ret;
		return "";
	}
	
	public static void main(String[] args) throws OWLOntologyCreationException{
		if (args.length != 2){
			System.out.println("Usage: java OWLToBratNormalizationFormat <input_ontology> <output_file>");
			return;
		}
		
		String ontologyURI = args[0];
		String outfile = args[1];
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
        // We load an ontology from the URI specified on the command line
        System.out.println("Loading ontology: " + ontologyURI);
        
        // Now load the ontology.
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyURI));
        
        // Get the classes used in the ontology.
        Set<OWLClass> classes = ontology.getClassesInSignature();
        
        PrintWriter writer;
		try {
			writer = new PrintWriter(outfile);
			
			System.out.println("Writing results to: " + outfile);
			
			for (OWLClass c : classes){
				String bc = bratifyClass(c, ontology);
				if (bc.length() > 0)
					writer.println(bc);
	        }
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
