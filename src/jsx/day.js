const datepattern = /^([0-9]{4})\/([0-9]{1,2})\/([0-9]{1,2})$/;

/**
 * @param {string} s 
    */
function parseDate(s) {
    if(s == null) {
        return null;
    }
    const m = s.match(datepattern);
    if(m == null) {
        return null;
    }
    const year = parseInt(m[1]);
    const month = parseInt(m[2]);
    const date = parseInt(m[3]);
    const d = new Date(year, month - 1, date);
    if(d.getFullYear() !== year || d.getMonth() + 1 !== month || d.getDate() !== date) {
        return null;
    }
    return d;
}

export { parseDate }

