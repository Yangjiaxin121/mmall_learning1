package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {

    String upLoad(MultipartFile file, String path);

}
