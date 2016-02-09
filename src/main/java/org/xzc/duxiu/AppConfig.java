package org.xzc.duxiu;

import java.sql.SQLException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xzc.duxiu.b0119.BookService;
import org.xzc.duxiu.model.BKBook;
import org.xzc.duxiu.model.Book;
import org.xzc.duxiu.model.Email;
import org.xzc.duxiu.model.ZxUrl;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

@Configuration
public class AppConfig {

	@Bean(destroyMethod = "close")
	public ConnectionSource connectionSource() throws SQLException, ClassNotFoundException {
		Class.forName( "org.sqlite.JDBC" );
		return new JdbcConnectionSource( "jdbc:sqlite:bilibili.db" );
	}

	@Bean(name = "bookDao")
	public RuntimeExceptionDao<Book, String> bookDao(ConnectionSource cs) throws SQLException {
		TableUtils.createTableIfNotExists( cs, Book.class );
		return new RuntimeExceptionDao<Book, String>( (Dao<Book, String>) DaoManager.createDao( cs, Book.class ) );
	}

	@Bean(name = "emailDao")
	public RuntimeExceptionDao<Email, Integer> emailDao(ConnectionSource cs) throws SQLException {
		TableUtils.createTableIfNotExists( cs, Email.class );
		return new RuntimeExceptionDao<Email, Integer>( (Dao<Email, Integer>) DaoManager.createDao( cs, Email.class ) );
	}

	@Bean(name = "zxUrlDao")
	public RuntimeExceptionDao<ZxUrl, String> zxUrlDao(ConnectionSource cs) throws SQLException {
		TableUtils.createTableIfNotExists( cs, ZxUrl.class );
		return new RuntimeExceptionDao<ZxUrl, String>( (Dao<ZxUrl, String>) DaoManager.createDao( cs, ZxUrl.class ) );
	}
	
	@Bean(name = "bkbookDao")
	public RuntimeExceptionDao<BKBook, String> bkbookDao(ConnectionSource cs) throws SQLException {
		TableUtils.createTableIfNotExists( cs, BKBook.class );
		return new RuntimeExceptionDao<BKBook, String>( (Dao<BKBook, String>) DaoManager.createDao( cs, BKBook.class ) );
	}

	@Bean
	public BookService bookService() {
		return new BookService();
	}
}
