package de.uni_passau.facultyinfo.server.dataloader;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.uni_passau.facultyinfo.server.dao.MetadataDAO;
import de.uni_passau.facultyinfo.server.dao.SportsCourseDAO;
import de.uni_passau.facultyinfo.server.dto.Metadata;
import de.uni_passau.facultyinfo.server.dto.SportsCourse;
import de.uni_passau.facultyinfo.server.dto.SportsCourseCategory;

public class SportsCourseLoader {
	public int load() {
		int status = 0;

		MetadataDAO metadataDAO = new MetadataDAO();
		Metadata sportsCourseMetadata = metadataDAO
				.getMetadata(Metadata.NAME_SPORTSCOURSE);

		if (sportsCourseMetadata != null
				&& sportsCourseMetadata.getSourceUrl() != null
				&& !sportsCourseMetadata.getSourceUrl().isEmpty()) {
			boolean returnValue = true;

			SportsCourseDAO sportsCourseDAO = new SportsCourseDAO();
			sportsCourseDAO.deleteAllSportsCourses();
			sportsCourseDAO.deleteAllSportsCourseCategories();

			Connection connection = Jsoup.connect(sportsCourseMetadata
					.getSourceUrl());
			connection.ignoreContentType(true);

			try {
				Document doc = connection.get();
				Elements sportsCourseCategoryElements = doc
						.select("dl.bs_menu dd");
				for (Element sportsCourseCategoryElement : sportsCourseCategoryElements) {
					sportsCourseCategoryElement = sportsCourseCategoryElement
							.select("a").get(0);
					String categoryId = UUID.randomUUID().toString();
					String categoryTitle = sportsCourseCategoryElement.text();
					String categoryUrl = sportsCourseMetadata.getSourceUrl()
							+ sportsCourseCategoryElement.attr("href");
					SportsCourseCategory category = new SportsCourseCategory(
							categoryId, categoryTitle);
					returnValue = returnValue
							&& sportsCourseDAO
									.createSportsCourseCategory(category);

					Connection subConnection = Jsoup.connect(categoryUrl);
					Document subDocument = subConnection.get();
					Elements sportsCourseElements = subDocument
							.select("table.bs_kurse tbody tr");
					for (Element sportsCourseElement : sportsCourseElements) {
						System.out.println(sportsCourseElement
								.select("td.bs_sknr").get(0).text());
						// Details
						String details = sportsCourseElement
								.select("td.bs_sdet").get(0).text();
						if (details.isEmpty()) {
							details = null;
						}

						// Number
						String number = sportsCourseElement
								.select("td.bs_sknr").get(0).text();

						// Day of Week
						String[] dayOfWeekStrings = sportsCourseElement
								.select("td.bs_stag").get(0).html()
								.split("<br />");
						ArrayList<ArrayList<Integer>> daysOfWeek = new ArrayList<ArrayList<Integer>>();
						for (String dayOfWeekString : dayOfWeekStrings) {
							dayOfWeekString = Jsoup.parse(dayOfWeekString)
									.text();
							if (!dayOfWeekString.isEmpty()) {
								daysOfWeek
										.add(parseDaysOfWeekString(dayOfWeekString));
							}
						}

						// Time
						ArrayList<Time> startTimes = new ArrayList<Time>();
						ArrayList<Time> endTimes = new ArrayList<Time>();
						if (!sportsCourseElement.select("td.bs_szeit").get(0)
								.text().isEmpty()) {
							String[] timeStrings = sportsCourseElement
									.select("td.bs_szeit").get(0).html()
									.split("<br />");

							for (String timeString : timeStrings) {
								timeString = Jsoup.parse(timeString).text();
								if (!timeString.isEmpty()) {
									String[] subStrings = timeString.split("-");
									startTimes
											.add(subStrings.length > 0 ? parseTime(subStrings[0])
													: null);
									endTimes.add(subStrings.length > 1 ? parseTime(subStrings[1])
											: null);
								}
							}
						}

						// Location
						ArrayList<String> locations = new ArrayList<String>();
						if (!sportsCourseElement.select("td.bs_sort").get(0)
								.text().isEmpty()) {
							String[] locationStrings = sportsCourseElement
									.select("td.bs_sort").get(0).html()
									.split("<br />");
							for (String location : locationStrings) {
								location = Jsoup.parse(location).text();
								if (!location.isEmpty()) {
									locations.add(location);
								}
							}
						}

						// Date
						String[] dateStrings = sportsCourseElement
								.select("td.bs_szr").get(0).text().split("-");
						SimpleDateFormat dateSdf = new SimpleDateFormat("d.MM.");
						Date startDate = null;
						Date endDate = null;
						try {
							startDate = dateSdf.parse(dateStrings[0]);
							endDate = dateStrings.length < 2 ? startDate
									: dateSdf.parse(dateStrings[1]);

							Calendar cal = Calendar.getInstance();
							cal.setTime(new Date());
							boolean winter = cal.get(Calendar.MONTH) >= 8
									|| cal.get(Calendar.MONTH) <= 2;
							int currentYear = cal.get(Calendar.YEAR);
							currentYear = winter && cal.get(Calendar.MONTH) < 8 ? currentYear - 1
									: currentYear;
							cal.setTime(startDate);
							cal.set(Calendar.YEAR,
									!winter || cal.get(Calendar.MONTH) >= 8 ? currentYear
											: currentYear + 1);
							startDate = cal.getTime();
							cal.setTime(endDate);
							cal.set(Calendar.YEAR,
									!winter || cal.get(Calendar.MONTH) >= 8 ? currentYear
											: currentYear + 1);
							endDate = cal.getTime();
						} catch (ParseException e) {
							e.printStackTrace();
						}

						// Host
						String host = sportsCourseElement.select("td.bs_skl")
								.get(0).text();
						if (host.isEmpty()) {
							host = null;
						}

						// Price
						String priceString = sportsCourseElement
								.select("td.bs_spreis").get(0).text();
						Double price = 0.0;
						if (priceString.matches(".+/.+")) {
							priceString = priceString.split("/")[0];
							price = Double.valueOf(priceString);
						}

						// Status
						String statusString = sportsCourseElement
								.select("td.bs_sbuch input,td.bs_sbuch span")
								.get(0).attr("class");
						int courseStatus = SportsCourse.STATUS_NOT_AVAILABLE;
						if (statusString.equals("bs_btn_buchen")) {
							courseStatus = SportsCourse.STATUS_OPEN;
						} else if (statusString.equals("bs_btn_ausgebucht")) {
							courseStatus = SportsCourse.STATUS_FULL;
						} else if (statusString.equals("bs_btn_ohne_anmeldung")) {
							courseStatus = SportsCourse.STATUS_NO_SIGNUP_REQUIRED;
						} else if (statusString.equals("bs_btn_abgelaufen")) {
							courseStatus = SportsCourse.STATUS_CLOSED;
						} else if (statusString.equals("bs_btn_warteliste")) {
							courseStatus = SportsCourse.STATUS_QUEUE;
						} else if (statusString.equals("bs_btn_nur_buero")) {
							courseStatus = SportsCourse.STATUS_OFFICE_SIGNUP;
						} else if (statusString.equals("bs_btn_storniert")) {
							courseStatus = SportsCourse.STATUS_STORNO;
						} else if (statusString.equals("bs_btn_keine_buchung")) {
							courseStatus = SportsCourse.STATUS_NO_SIGNUP_POSSIBLE;
						}

						// Create Sports Course
						int index = 0;
						if (daysOfWeek.isEmpty()) {
							ArrayList<Integer> dummyList = new ArrayList<Integer>();
							dummyList.add(SportsCourse.DATE_NOT_AVAILABLE);
							daysOfWeek.add(dummyList);
						}
						for (ArrayList<Integer> subDaysOfWeek : daysOfWeek) {
							Time startTime = startTimes.size() >= index + 1 ? startTimes
									.get(index) : null;
							Time endTime = endTimes.size() >= index + 1 ? endTimes
									.get(index) : null;
							String location = locations.size() >= index + 1 ? locations
									.get(0) : null;
							for (Integer dayOfWeek : subDaysOfWeek) {
								// Id
								String id = UUID.randomUUID().toString();

								SportsCourse sportsCourse = new SportsCourse(
										id, category, number, details,
										dayOfWeek, startTime, endTime,
										location, startDate, endDate, host,
										price, courseStatus);
								returnValue = returnValue
										&& sportsCourseDAO
												.createSportsCourse(sportsCourse);
							}
							index++;
						}
					}
				}
			} catch (IOException e) {
				returnValue = false;
				e.printStackTrace();
			}
			status = returnValue ? 0 : 1;
		} else {
			status = 2;
		}

		metadataDAO.updateStatuscode(Metadata.NAME_SPORTSCOURSE, status);

		return status;
	}

	private ArrayList<Integer> parseDaysOfWeekString(String dayOfWeekString) {
		ArrayList<Integer> daysOfWeek = new ArrayList<Integer>();
		String[] subStrings = dayOfWeekString.split("-");
		if (subStrings.length == 2) {
			int firstDay = parseDayOfWeek(subStrings[0]);
			int lastDay = parseDayOfWeek(subStrings[1]);
			for (int i = firstDay; i <= lastDay; i++) {
				daysOfWeek.add(i);
			}
		} else if (subStrings.length == 1) {
			daysOfWeek.add(parseDayOfWeek(subStrings[0]));
		}
		return daysOfWeek;
	}

	private int parseDayOfWeek(String dayOfWeekString) {
		int dayOfWeek = SportsCourse.DATE_NOT_AVAILABLE;
		if (dayOfWeekString.equals("Mo")) {
			dayOfWeek = SportsCourse.MONDAY;
		} else if (dayOfWeekString.equals("Di")) {
			dayOfWeek = SportsCourse.TUESDAY;
		} else if (dayOfWeekString.equals("Mi")) {
			dayOfWeek = SportsCourse.WEDNESDAY;
		} else if (dayOfWeekString.equals("Do")) {
			dayOfWeek = SportsCourse.THURSDAY;
		} else if (dayOfWeekString.equals("Fr")) {
			dayOfWeek = SportsCourse.FRIDAY;
		} else if (dayOfWeekString.equals("Sa")) {
			dayOfWeek = SportsCourse.SATURDAY;
		} else if (dayOfWeekString.equals("So")) {
			dayOfWeek = SportsCourse.SUNDAY;
		}
		return dayOfWeek;
	}

	private Time parseTime(String timeString) {
		try {
			SimpleDateFormat timeSdf = new SimpleDateFormat("H:m");
			Time time = new Time(timeSdf.parse(timeString).getTime());
			return time;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
}
