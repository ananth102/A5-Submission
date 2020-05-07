import numpy as np
import librosa
import tensorflow as tf

from sklearn.model_selection import train_test_split
from tensorflow.keras import layers, models



def extract_features(file_name):
   
    try:
        audio, sample_rate = librosa.load(file_name, res_type='kaiser_fast') 
        audio = audio[:22050]
        #mfccs = librosa.feature.melspectrogram(y=audio, sr=sample_rate, n_mfcc=40)
        mfccs = librosa.feature.melspectrogram(y=audio, sr=sample_rate,power=2)
        #mfccsscaled = np.mean(mfccs.T,axis=0)
        
    except Exception as e:
        print("Error encountered while parsing file: ",file_name,e)
        return None 
     
    return mfccs

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
    for i in range(100):
        nonCough = extract_features("Regen_coofs/neg"+str(i)+".wav")
        featureArr.append(nonCough)
        labelsArr.append(1)
    return featureArr,labelsArr


def train(xTrain,yTrain,xTest,yTest):
    xTrain = np.reshape(xTrain,(np.shape(xTrain)[0] ,128,44,1))
    xTest= np.reshape(xTest,(np.shape(xTest)[0] ,128,44,1))
    
    model = models.Sequential()
    model.add(layers.Conv2D(32, (3, 3), activation='relu', input_shape=(128,44,1)))
    model.add(layers.MaxPooling2D((2, 2)))
    model.add(layers.Conv2D(64, (3, 3), activation='relu'))
    model.add(layers.MaxPooling2D((2, 2)))
    model.add(layers.Conv2D(64, (3, 3), activation='relu'))
    model.add(layers.Flatten())
    model.add(layers.Dense(100, activation='relu'))
    model.add(layers.Dense(20))
    model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
              metrics=['accuracy'])

    history = model.fit(xTrain, yTrain, epochs=10,validation_data=(xTest, yTest))
    test_loss, test_acc = model.evaluate(xTest,  yTest, verbose=2)
    tf.saved_model.save(model,"woof/")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    # Save the TF Lite model as file
    f = open('wof.tflite', "wb")
    f.write(tflite_model)
    f.close()
    ft = extract_features("r928.wav")
    ft  = np.reshape(ft,(1 ,128,44,1))
    woof = model.predict(ft)
    print(woof)
    #builder.save()
    pass

#t = extract_features("r6.wav")
xArr,yArr = extractWavFeats()
xArr = np.array(xArr)
yArr = np.array(yArr)
x_train, x_test, y_train, y_test = train_test_split(xArr[:], yArr[:], test_size=0.2, random_state = 42)
train(x_train,y_train,x_test,y_test)