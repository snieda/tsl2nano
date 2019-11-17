function fitness() {
    var exptected = 10;
    var BAD = 9999999.0;
    if (input.length != 8)
        return BAD;
    var sum = 0;
    for(i=0; i<input.length; i+=2) {
        sum += input[i] * Math.pow(input[i+1], i);
    }
    return  Math.abs(sum - exptected);
}

fitness();