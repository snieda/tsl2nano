package de.tsl2.nano.aspect;

class Account implements IAccount {

    //@Cover(up = true)
    @Override
    public boolean allowed() {
        return true;
    }
}
