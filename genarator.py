from praatio import tgio
import os
from scipy import signal
from scipy.io import wavfile
import matplotlib.pyplot as plt
from pydub import AudioSegment
import wave
import pylab
import numpy as np
import librosa

cooughPtr = 0
nonCoughPtr = 0


#returns a (list of files)
def getFileList():
    dataFolder = "cof"
    files = os.listdir(dataFolder)
    fullpth = []
    for fil in files:
        full_path = os.path.join(dataFolder, fil)
        fullpth.append(full_path)
    return fullpth
    pass


def graph_spectrogram(wav_file):
    sound_info, frame_rate = get_wav_info(wav_file)
    pylab.figure(num=None, figsize=(19, 12))
    pylab.subplot(111)
    pylab.title('spectrogram of %r' % wav_file)
    pylab.specgram(sound_info, Fs=frame_rate)
    pylab.savefig('spectrogram.png')
def get_wav_info(wav_file):
    wav = wave.open(wav_file, 'r')
    frames = wav.readframes(-1)
    sound_info = pylab.fromstring(frames, 'int16')
    frame_rate = wav.getframerate()
    wav.close()
    return sound_info, frame_rate

# def ffs(featureMatrix):
#     maxScore 





#returns 2 arrays of split wavs and labels
def splitWavs(audio_file,textgrid):
    global cooughPtr
    global nonCoughPtr
    intervals = []
    if "word" in textgrid.tierDict:
        intervals = textgrid.tierDict["word"].entryList
    elif "words" in textgrid.tierDict:
        intervals = textgrid.tierDict["words"].entryList
    else:
        return
    threshold = 1.00
    sound = AudioSegment.from_file(audio_file)
    name = audio_file[:-4]
    count = 0
    for interval in intervals:
        if interval.end - interval.start < threshold:continue
        intr = sound[interval.start*1000:((1+interval.start)*1000)]
        if interval.label == "cough":
            intr.export("Regen_cofs/cough"+str(cooughPtr)+".wav", format="wav")
            cooughPtr+=1
        else:
            intr.export("Regen_cofs/neg"+str(nonCoughPtr)+".wav", format="wav")
            nonCoughPtr+=1
        
        break

    pass

#creates Spectograms given a file list writes images and tuples(labels) to a folder
def chopAudio(fileList):
    texgridList = os.listdir("flusense_data")
    compatible = 0
    for f in fileList:
        woof = f.split("/")[1][:-4] +".TextGrid"
        if woof not in texgridList:continue
        compatible+=1
        full_path = os.path.join("flusense_data", woof)
        tg = tgio.openTextgrid(full_path)
        splitWavs(f,tg)
        
    print(compatible," samples")

chopAudio(getFileList())