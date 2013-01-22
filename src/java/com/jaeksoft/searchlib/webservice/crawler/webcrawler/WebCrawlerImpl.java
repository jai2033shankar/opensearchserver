/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2011-2012 Emmanuel Keller / Jaeksoft
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
package com.jaeksoft.searchlib.webservice.crawler.webcrawler;

import java.io.IOException;

import javax.naming.NamingException;
import javax.xml.ws.WebServiceException;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.ClientCatalog;
import com.jaeksoft.searchlib.ClientFactory;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.crawler.web.process.WebCrawlMaster;
import com.jaeksoft.searchlib.webservice.CommonResult;
import com.jaeksoft.searchlib.webservice.CommonServices;
import com.jaeksoft.searchlib.webservice.crawler.CrawlerUtils;
import com.jaeksoft.searchlib.webservice.crawler.CrawlerUtils.CrawlerActionEnum;

public class WebCrawlerImpl extends CommonServices implements WebCrawler {

	@Override
	public CommonResult webCrawler(String use, String login, String key,
			CrawlerActionEnum action, int timeOut, boolean runOnce) {
		try {
			ClientFactory.INSTANCE.properties.checkApi();
			if (timeOut == 0)
				timeOut = 1200;
			if (isLoggedWebStartStop(use, login, key)) {
				Client client = ClientCatalog.getClient(use);
				WebCrawlMaster webCrawlMaster = client.getWebCrawlMaster();
				return CrawlerUtils.crawlerAction(webCrawlMaster, timeOut,
						runOnce, action);
			}
		} catch (SearchLibException e) {
			new WebServiceException(e);
		} catch (NamingException e) {
			new WebServiceException(e);
		} catch (InterruptedException e) {
			new WebServiceException(e);
		} catch (IOException e) {
			new WebServiceException(e);
		}
		return new CommonResult(false, "Something went Wrong");

	}

}