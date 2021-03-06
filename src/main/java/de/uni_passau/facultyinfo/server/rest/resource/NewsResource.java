package de.uni_passau.facultyinfo.server.rest.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.uni_passau.facultyinfo.server.dao.NewsDAO;
import de.uni_passau.facultyinfo.server.dataloader.NewsLoader;
import de.uni_passau.facultyinfo.server.dto.News;

@Path("/news")
public class NewsResource {
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<News> getNewsList() {
		NewsDAO newsDAO = new NewsDAO();
		return newsDAO.getNewsList();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public News getArticle(@PathParam("id") String id) {
		NewsDAO newsDAO = new NewsDAO();
		News news = newsDAO.getNews(id);
		if (news == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		return news;
	}

	@GET
	@Path("/load")
	@Produces(MediaType.TEXT_PLAIN)
	public int loadNews() {
		NewsLoader newsLoader = new NewsLoader();
		return newsLoader.load();
	}
}