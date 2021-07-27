import numpy as np
from cv2 import cv2 as cv

def greet(name):
    print("hello," , name)

def add(a,b):
    return a + b

def drawMatchesKnn_cv(img1_gray,kp1,img2_gray,kp2,goodMatch):
    h1, w1 = img1_gray.shape[:2]
    h2, w2 = img2_gray.shape[:2]

    vis = np.zeros((max(h1, h2), w1 + w2, 3), np.uint8)
    vis[:h1, :w1] = img1_gray
    vis[:h2, w1:w1 + w2] = img2_gray

    p1 = [kpp.queryIdx for kpp in goodMatch]
    p2 = [kpp.trainIdx for kpp in goodMatch]

    post1 = np.int32([kp1[pp].pt for pp in p1])
    post2 = np.int32([kp2[pp].pt for pp in p2]) + (w1, 0)

    for (x1, y1), (x2, y2) in zip(post1, post2):
        cv.line(vis, (x1, y1), (x2, y2), (0,0,255))

    cv.namedWindow("match",cv.WINDOW_NORMAL)
    cv.imshow("match", vis)

def find_corner_point(realtimepath,referancepath):

    realtime = cv.imread(realtimepath)
    referance = cv.imread(referancepath)
    print(realtime.shape,referance.shape)
    h,w = realtime.shape[:2]
    h1,w1 = referance.shape[:2]

    realtime = cv.resize(realtime,(int(w/8),int(h/8)),interpolation = cv.INTER_CUBIC)
    referance = cv.resize(referance,(int(w1/8),int(h1/8)),interpolation = cv.INTER_CUBIC)

    h,w = realtime.shape[:2]
    h1,w1 = referance.shape[:2]
    print(realtime.shape,referance.shape)

    start = time.time()
    sift = cv.xfeatures2d.SURF_create()

    kp1, des1 = sift.detectAndCompute(realtime, None)
    kp2, des2 = sift.detectAndCompute(referance, None)

    FLANN_INDEX_KDTREE=0
    indexParams=dict(algorithm=FLANN_INDEX_KDTREE,trees=5)
    searchParams= dict(checks=50)
    flann=cv.FlannBasedMatcher(indexParams,searchParams)
    matches=flann.knnMatch(des1,des2,k=2)

    end = time.time()
    goodMatch = []
    for m,n in matches:
        if m.distance < 0.5*n.distance:
            goodMatch.append(m)

    src_pts = np.array([ kp1[m.queryIdx].pt for m in goodMatch])    #查询图像的特征描述子索引
    dst_pts = np.array([ kp2[m.trainIdx].pt for m in goodMatch])    #训练(模板)图像的特征描述子索引
    H=cv.findHomography(src_pts,dst_pts,method=cv.RANSAC)         #生成变换矩阵
    M = H[0]

    pts = np.float32([ [(w-1)/2,(h-1)/2] ]).reshape(-1,1,2)
    pts1 = np.float32([[(3000,1500)]]).reshape(-1,1,2)
    dst = cv.perspectiveTransform(pts,M)
    dst1 = cv.perspectiveTransform(pts1,M)
    dst1 = dst1[0][0]

    dst_referance = dst[0][0]
    dst1_referance = dst1
    factor = (1<<2)

    cv.circle(referance,(int(dst1_referance[0]*factor+0.5),int(dst1_referance[1]*factor+0.5)),5,color = (0,0,255),thickness=5,shift=2)
    cv.circle(realtime,(3000,1500),5,color = (0,0,255),thickness=5,shift=2)

    cen_referance = [(w1-1)/2,(h1-1)/2]

    pixdis = math.sqrt((cen_referance[0]-dst_referance[0])**2+(cen_referance[1]-dst_referance[1])**2)

    gpsinfo1 = exifinfo.getgpsinfo(realtimepath)
    altinfo1 = gpsinfo1[2]
    gpsinfo2 = exifinfo.getgpsinfo(referancepath)
    altinfo2 = gpsinfo2[2]
    gpsdis = exifinfo.calcDistance(gpsinfo1,gpsinfo2)
    B = gpsdis

    perpix = gpsdis/pixdis
    f = (1/perpix)*altinfo2
    print("time =",end-start)


    cv.imshow("img1",realtime)
    cv.imshow("img2",referance)
    drawMatchesKnn_cv(realtime,kp1,referance,kp2,goodMatch[:800])#:20

    return dst_referance,perpix,M,f,B