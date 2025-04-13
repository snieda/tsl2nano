for f in screenshots/* ; do convert "$f" -resize 640x600 "${f.*}".jpg ; done
