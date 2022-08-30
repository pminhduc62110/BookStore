package com.example.bookstoreonline.service.impl;

import com.example.bookstoreonline.config.FileStorageProperties;
import com.example.bookstoreonline.dto.UploadBookDTO;
import com.example.bookstoreonline.model.Book;
import com.example.bookstoreonline.model.Category;
import com.example.bookstoreonline.repository.BookRepository;
import com.example.bookstoreonline.repository.CategoryRepository;
import com.example.bookstoreonline.service.IBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BookServiceImpl implements IBookService {
    @Value("${page.size}")
    private int pageSize;
    @Value("${page.category.book.size}")
    private int pageCategoryBookSize;
    private final Path fileStorageLocation;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public BookServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @Override
    public Page<Book> getAllBooks(int pageNum) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        return bookRepository.findAll(pageable);
    }

    @Override
    public Book getBookById(Long bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if(bookOpt.isEmpty()) {
            return null;
        }
        log.info("Book with id {}: {}", bookId, bookOpt.get());
        return bookOpt.get();
    }

    @Override
    public Page<Book> getBooksByCategoryId(Long categoryId, int pageNum) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageCategoryBookSize);
        Page<Book> page = bookRepository.findByCategory_Id(categoryId, pageable);
        log.info("Book with category id {}: {}", categoryId, page.getContent());
        return page;
    }

    @Override
    public void addNewBook(UploadBookDTO uploadBookDTO, MultipartFile file) {
        Category category = categoryRepository.getCategoryByCategoryName(uploadBookDTO.getCategory());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {
            Path targetLocation  = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Book book = Book.builder()
                .name(uploadBookDTO.getName())
                .about(uploadBookDTO.getAbout())
                .language(uploadBookDTO.getLanguage())
                .category(category)
                .author(uploadBookDTO.getAuthor())
                .publisher(uploadBookDTO.getPublisher())
                .publishDate(LocalDate.parse(uploadBookDTO.getPublishDate(), formatter))
                .quantity(uploadBookDTO.getQuantity())
                .price(uploadBookDTO.getPrice())
                .image("/img/" + fileName)
                .discount(uploadBookDTO.getDiscount())
                .build();
        bookRepository.save(book);
    }

    @Override
    public boolean editBook(UploadBookDTO editBookDTO, Long bookId) {
        Category category = categoryRepository.getCategoryByCategoryName(editBookDTO.getCategory());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Book editBook = Book.builder()
                .id(bookId)
                .name(editBookDTO.getName())
                .language(editBookDTO.getLanguage())
                .category(category)
                .author(editBookDTO.getAuthor())
                .publisher(editBookDTO.getPublisher())
                .publishDate(LocalDate.parse(editBookDTO.getPublishDate(), formatter))
                .quantity(editBookDTO.getQuantity())
                .price(editBookDTO.getPrice())
                .discount(editBookDTO.getDiscount() / 100)
                .build();
        Book editedBook = bookRepository.save(editBook);
        return editedBook.equals(editBook);
    }

    @Override
    public Page<Book> searchAllBooksByName(int pageNum, String name) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Book> page = bookRepository.findByNameContaining(name, pageable);
        if(!page.hasContent()) {
            return null;
        }
        return page;
    }


}
