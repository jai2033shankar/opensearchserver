package com.jaeksoft.searchlib.facet;

import java.io.IOException;

import org.apache.lucene.index.TermDocs;

import com.jaeksoft.searchlib.index.DocSetHits;
import com.jaeksoft.searchlib.index.ReaderLocal;
import com.jaeksoft.searchlib.result.ResultSearch;

public class FacetMultivalued extends FacetSearch {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3251943561731498334L;

	public FacetMultivalued(ResultSearch result, FacetField facetField)
			throws IOException {
		super(facetField);
		ReaderLocal reader = result.getReader();
		setReader(reader);
		this.count = new int[stringIndex.lookup.length];
		String fieldName = facetField.getName();
		DocSetHits dsh = result.getDocSetHits();
		int i = 0;
		for (String term : stringIndex.lookup) {
			if (term != null) {
				TermDocs termDocs = reader.getTermDocs(fieldName, term);
				while (termDocs.next())
					if (termDocs.freq() > 0)
						if (dsh.contains(termDocs.doc()))
							count[i]++;
			}
			i++;
		}

	}
}
