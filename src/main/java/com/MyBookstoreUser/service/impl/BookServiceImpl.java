package com.MyBookstoreUser.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.MyBookstoreUser.model.Book;
import com.MyBookstoreUser.repository.BookRepository;
import com.MyBookstoreUser.service.BookService;

@Service
public class BookServiceImpl implements BookService{
	
	@Autowired
	private BookRepository bookRepository;
	
	public List<Book> findAll() {
		List<Book> bookList = (List<Book>) bookRepository.findAll();
		List<Book> activeBookList = new ArrayList<>();
		
		for (Book book: bookList) {
			if(book.isActive()) {
				activeBookList.add(book);
			}
		}
		
		return activeBookList;
	}

	@Override
	public Optional<Book> findOne(Long id) {
		return bookRepository.findById(id);
	}
	
}