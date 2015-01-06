/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2015 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.crawler.database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.analysis.LanguageEnum;
import com.jaeksoft.searchlib.crawler.FieldMapContext;
import com.jaeksoft.searchlib.crawler.common.process.CrawlStatus;
import com.jaeksoft.searchlib.index.IndexDocument;
import com.jaeksoft.searchlib.util.InfoCallback;
import com.jaeksoft.searchlib.util.ReadWriteLock;
import com.jaeksoft.searchlib.util.Variables;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class DatabaseCrawlMongoDbThread extends DatabaseCrawlThread {

	private final ReadWriteLock rwl = new ReadWriteLock();

	private final DatabaseCrawlMongoDb databaseCrawl;

	public DatabaseCrawlMongoDbThread(Client client,
			DatabaseCrawlMaster crawlMaster,
			DatabaseCrawlMongoDb databaseCrawl, Variables variables,
			InfoCallback infoCallback) {
		super(client, crawlMaster, databaseCrawl, infoCallback);
		this.databaseCrawl = (DatabaseCrawlMongoDb) databaseCrawl.duplicate();
		this.databaseCrawl.applyVariables(variables);
	}

	private boolean index(List<IndexDocument> indexDocumentList, int limit)
			throws NoSuchAlgorithmException, IOException, URISyntaxException,
			SearchLibException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException {
		int i = indexDocumentList.size();
		if (i == 0 || i < limit)
			return false;
		setStatus(CrawlStatus.INDEXATION);
		client.updateDocuments(indexDocumentList);
		rwl.w.lock();
		try {
			pendingIndexDocumentCount -= i;
			updatedIndexDocumentCount += i;
		} finally {
			rwl.w.unlock();
		}
		indexDocumentList.clear();
		if (infoCallback != null)
			infoCallback.setInfo(updatedIndexDocumentCount
					+ " document(s) indexed");
		sleepMs(databaseCrawl.getMsSleep());
		return true;
	}

	final private void runner_update(DBCursor cursor)
			throws SearchLibException, NoSuchAlgorithmException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, IOException, URISyntaxException,
			SQLException {
		final int bf = databaseCrawl.getBufferSize();
		cursor.batchSize(bf);
		DatabaseFieldMap databaseFieldMap = databaseCrawl.getFieldMap();
		List<IndexDocument> indexDocumentList = new ArrayList<IndexDocument>(0);
		LanguageEnum lang = databaseCrawl.getLang();
		FieldMapContext context = new FieldMapContext(client, lang);

		while (cursor.hasNext() && !isAborted()) {
			IndexDocument indexDocument = new IndexDocument(lang);
			databaseFieldMap.mapDBRrsult(context, cursor, indexDocument);
			indexDocumentList.add(indexDocument);
			pendingIndexDocumentCount++;
			if (index(indexDocumentList, bf))
				setStatus(CrawlStatus.CRAWL);
		}
		index(indexDocumentList, 0);
	}

	@Override
	public void runner() throws Exception {
		setStatus(CrawlStatus.STARTING);

		MongoClient mongoClient = null;
		try {
			mongoClient = databaseCrawl.getMongoClient();
			DBCollection collection = databaseCrawl.getCollection(mongoClient);
			DBObject ref = databaseCrawl.getRefObject();
			DBObject key = databaseCrawl.getKeyObject();
			DBCursor cursor = collection.find(ref, key);
			try {
				setStatus(CrawlStatus.CRAWL);
				if (cursor != null)
					runner_update(cursor);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			if (updatedIndexDocumentCount > 0 || updatedDeleteDocumentCount > 0)
				client.reload();
		} finally {
			if (mongoClient != null)
				mongoClient.close();
		}

	}
}