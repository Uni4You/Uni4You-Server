package de.uni_passau.facultyinfo.server.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.uni_passau.facultyinfo.server.dao.connection.AttributeContainer;
import de.uni_passau.facultyinfo.server.dao.connection.JDBCConnection;
import de.uni_passau.facultyinfo.server.dto.News;

public class NewsDAO {

	public List<News> getNewsList() {
		ResultSet resultSet = JDBCConnection.getInstance().executeSelect(
				"SELECT id, title, description, publishingdate FROM news");
		if (resultSet == null) {
			return null;
		}

		try {
			return mapResultSetToNewsList(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public News getNews(String id) {
		ArrayList<String> attributes = new ArrayList<String>();
		attributes.add(id);
		ResultSet resultSet = JDBCConnection
				.getInstance()
				.executeSelect(
						"SELECT id, title, description, url, text, publishingdate FROM news WHERE id = ?",
						attributes);
		if (resultSet == null) {
			return null;
		}

		try {
			return mapResultSetToNews(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean createNews(News news) {
		AttributeContainer attributes = new AttributeContainer();
		attributes.addString(1, news.getId());
		attributes.addString(2, news.getTitle());
		attributes.addString(3, news.getDescription());
		attributes.addString(4, news.getUrl());
		attributes.addString(5, news.getText());
		attributes.addDateTime(6, news.getPublicationDate());
		return JDBCConnection
				.getInstance()
				.executeStatement(
						"INSERT INTO news (id, title, description, url, text, publishingdate) VALUES (?, ?, ?, ?, ?, ?)",
						attributes) == 1;
	}

	private ArrayList<News> mapResultSetToNewsList(ResultSet resultSet)
			throws SQLException {
		ArrayList<News> newsList = new ArrayList<News>();
		while (resultSet.next()) {
			News news = new News(resultSet.getString("id"),
					resultSet.getString("title"),
					resultSet.getString("description"), null, null,
					resultSet.getTimestamp("publishingDate"));
			newsList.add(news);
		}

		return newsList;
	}

	private News mapResultSetToNews(ResultSet resultSet) throws SQLException {
		if (resultSet.next()) {
			News news = new News(resultSet.getString("id"),
					resultSet.getString("title"),
					resultSet.getString("description"),
					resultSet.getString("url"), resultSet.getString("text"),
					resultSet.getTimestamp("publishingDate"));
			return news;
		}
		return null;
	}

	public void deleteAllNews() {
		JDBCConnection.getInstance().executeStatement("DELETE FROM news");
	}

}
