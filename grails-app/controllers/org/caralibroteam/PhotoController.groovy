package org.caralibroteam

import org.springframework.dao.DataIntegrityViolationException

import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile
import org.springframework.web.multipart.*

class PhotoController {
	PhotoUploadService photoUploadService
    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        //redirect(action: "list", params: params)
		def photos=Photo.findAll();
		return [photos:photos]
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        [photoInstanceList: Photo.list(params), photoInstanceTotal: Photo.count()]
    }

    def create() {
        [photoInstance: new Photo(params)]
    }

    def save() {
				
		def date = new Date()
		//def fDate = date.format('yyyy-MM-dd')
		def hashTitle = params.getAt('title').hashCode()
		def photoInstance = new Photo(name:"${hashTitle}.png", title:params.getAt('title'), dateUploaded:date)
        if (!photoInstance.save(flush: true)) {
            render(view: "create", model: [photoInstance: photoInstance])
            return
        }

		if(request instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest mpr = (MultipartHttpServletRequest)request;
			CommonsMultipartFile f = (CommonsMultipartFile) mpr.getFile("photo");
			if (!f.isEmpty()) {
				  photoUploadService.uploadFile(f, "${hashTitle}.png", "Photos")
			}
		}
		
        flash.message = message(code: 'default.created.message', args: [message(code: 'photo.label', default: 'Photo'), photoInstance.id])
        redirect(action: "show", id: photoInstance.id)
    }

    def show(Long id) {
        def photoInstance = Photo.get(id)
        if (!photoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "list")
            return
        }

        [photoInstance: photoInstance]
    }

    def edit(Long id) {
        def photoInstance = Photo.get(id)
        if (!photoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "list")
            return
        }

        [photoInstance: photoInstance]
    }

    def update(Long id, Long version) {
        def photoInstance = Photo.get(id)
        if (!photoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (photoInstance.version > version) {
                photoInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'photo.label', default: 'Photo')] as Object[],
                          "Another user has updated this Photo while you were editing")
                render(view: "edit", model: [photoInstance: photoInstance])
                return
            }
        }

        photoInstance.properties = params

        if (!photoInstance.save(flush: true)) {
            render(view: "edit", model: [photoInstance: photoInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'photo.label', default: 'Photo'), photoInstance.id])
        redirect(action: "show", id: photoInstance.id)
    }

    def delete(Long id) {
        def photoInstance = Photo.get(id)
        if (!photoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "list")
            return
        }

        try {
            photoInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'photo.label', default: 'Photo'), id])
            redirect(action: "show", id: id)
        }
    }
}
