from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier as rf
from sklearn.neighbors import KNeighborsClassifier as kn
from sklearn.feature_selection import RFE
from sklearn_porter import Porter
import numpy as np
import librosa

def extract_features(file_name):
   
    try:
        audio, sample_rate = librosa.load(file_name, res_type='kaiser_fast') 
        mfccs = librosa.feature.mfcc(y=audio, sr=sample_rate, n_mfcc=40)
        mfccsscaled = np.mean(mfccs.T,axis=0)
        
    except Exception as e:
        print("Error encountered while parsing file: ",file_name)
        return None 
     
    return mfccsscaled

def extractWavFeats():
    featureArr = []
    labelsArr = []
    for i in range(43):
        coughfeat = extract_features("Regen_coofs/cough"+str(i)+".wav")
        nonCough = extract_features("Regen_coofs/neg"+str(i)+".wav")
        featureArr.append(coughfeat)
        featureArr.append(nonCough)
        labelsArr.append(0)
        labelsArr.append(1)
    for i in range(30):
        nonCough = extract_features("Regen_coofs/neg"+str(i)+".wav")
        featureArr.append(nonCough)
        labelsArr.append(1)
    return featureArr,labelsArr


#Optional Load
xArr = np.genfromtxt("x.csv",delimiter=",")
yArr = np.genfromtxt("y.csv",delimiter=",")
x_train, x_test, y_train, y_test = train_test_split(xArr[:], yArr[:], test_size=0.2, random_state = 42)
print("Done Splitting")

forest = rf(max_depth=10)
selector = RFE(forest,10,1)
fit = selector.fit(x_train,y_train)
print(fit.score(xArr[0:80],yArr[0:80]))
