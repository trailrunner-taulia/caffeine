package com.github.benmanes.caffeine.cache;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.clearspring.analytics.stream.StreamSummary;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class Indicator<K> {
  static final int K = 70;

  private final FrequencySketch<K> sketch;
  private final EstSkew<K> estSkew;
  private final Hinter hinter;
  private long sample;

  public Indicator(long maximumSize) {
    this.sample = 0;
    this.hinter = new Hinter();
    this.estSkew = new EstSkew<>();
    this.sketch = new FrequencySketch<>();
    this.sketch.ensureCapacity(maximumSize);
  }

  public void record(K key) {
    int hint = sketch.frequency(key);
    hinter.increment(hint);
    sketch.increment(key);
    estSkew.record(key);
    sample++;
  }

  public void reset() {
    hinter.reset();
    estSkew.reset();
    sample = 0;
  }

  public long getSample() {
    return sample;
  }

  public double getSkew() {
    return estSkew.estSkew(K);
  }

  public double getHint() {
    return hinter.getAverage();
  }

  public double getIndicator() {
    double skew = getSkew();
    return (getHint()) * (skew < 1 ? 1 - Math.pow(skew, 3) : 0) / 15.0;
  }

  private static class Hinter {
    int sum;
    int count;
    int[] freq = new int[16];

    public Hinter() {}

    public void increment(int i) {
      sum += i;
      count++;
      freq[i]++;
    }

    public void reset() {
      sum = count = 0;
      Arrays.fill(freq, 0);
    }

    public int getMedian() {
      int mid = (1 + IntStream.of(freq).sum()) / 2;
      int count = 0;
      for (int i = 0; i < 16; i++) {
        count += freq[i];
        if (count >= mid) {
          return i;
        }
      }
      return 0;
    }

    public double getAverage() {
      return ((double) sum) / ((double) count);
    }

    public int getSum() {
      return sum;
    }

    public int getCount() {
      return count;
    }

    public int[] getFreq() {
      return freq;
    }
  }

  private static class EstSkew<K> {
    Long2IntMap freq;
    StreamSummary<K> stream;

    public EstSkew() {
      this.freq = new Long2IntOpenHashMap();
      this.freq.defaultReturnValue(0);
      this.stream = new StreamSummary<>(1000);
    }

    public void record(K key) {
      // freq.put(key, freq.get(key) + 1);
      stream.offer(key);
    }

    public void reset() {
      freq.clear();
      this.stream = new StreamSummary<>(1000);
    }

    public IntStream getTopK(int k) {
      // return freq.values().stream().sorted(Collections.reverseOrder()).mapToInt(i->i).limit(k);
      return stream.topK(k).stream().mapToInt(counter -> (int) counter.getCount());
    }

    public double estSkew(int k) {
      SimpleRegression regression = new SimpleRegression();
      int[] idx = {1};
      getTopK(k).forEachOrdered(freq -> regression.addData(Math.log(idx[0]++), Math.log(freq)));
      return -regression.getSlope();
    }

    public int[] getFreq() {
      return freq.values().stream().sorted().mapToInt(i -> i).toArray();
    }

    public int getUniques() {
      return freq.size();
    }
  }

  public long getGini() {
    int[] a = estSkew.getFreq();
    double n = a.length;
    double ginisum = 0;
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      ginisum += 2.0 * (i + 1) * a[i];
      sum += a[i];

    }
    ginisum = (ginisum / (sum * n)) - (n + 1) / n;
    return (long) (ginisum * 1000);
  }

  public long getEntropy() {
    int[] a = estSkew.getFreq();
    double entsum = 0;
    double sum = IntStream.of(a).sum();
    for (int i = 0; i < a.length; i++) {
      double x = a[i] / sum;
      entsum += (x) * Math.log(x);
    }
    return (long) (-entsum * 1000);
  }

  public int[] getFreqs() {
    return hinter.getFreq();
  }

  public int getUniques() {
    return estSkew.getUniques();
  }
}
